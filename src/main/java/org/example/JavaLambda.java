package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOError;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Hello world!
 *
 */
public class JavaLambda implements RequestHandler<Map<String, Object>,String>
{
    private static HashMap<String, String> Categorias = new HashMap<String,String>(Map.of(
            "Alimentación","5e2de8a7-bf9d-8253-8d90-81411e1bf137",
            "Ajuste","b42de8a7-bf9d-8369-bf17-01ab3eabe64c",
            "Educación","4aade8a7-bf9d-82d7-a34a-0187e69273ed",
            "Deporte","e7ade8a7-bf9d-834f-afab-81d4860b237f",
            "Compras","611de8a7-bf9d-83c3-aead-013ed9b51c0d",
            "Transporte","55ade8a7-bf9d-838d-87ae-01552b7da65c",
            "Suscripciones","a4fde8a7-bf9d-8286-b520-81af03df03cd"
    ));
    private static HashMap<String, String> Cuentas = new HashMap<String,String>(Map.of(
            "Nequi","392de8a7-bf9d-832c-bf2a-81c953edb1da",
            "Tarjeta de crédito","797de8a7-bf9d-831a-936c-814cc3b22bb4",
            "Efectivo","cf8de8a7-bf9d-8248-b8c0-8135c6db95ba"
    ));
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String DATA_SOURCE_ID = "b10de8a7-bf9d-8241-a7dd-07abbda2cf6e";
    private static final String NOTION_TOKEN = System.getenv("NOTION_TOKEN");
    private static final HttpClient client = HttpClient.newHttpClient();

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        Input input = null;
        try {
            // Obtener el body enviado por API Gateway
            String body = (String) event.get("body");

            context.getLogger().log("BODY: " + body);

            // Convertir el JSON del body a Input
            input = mapper.readValue(body, Input.class);

            context.getLogger().log("Concepto: " + input.getConcepto());
            context.getLogger().log("Cuenta: " + input.getCuenta());
            context.getLogger().log("Categoría: " + input.getCategoría());
            context.getLogger().log("Valor: " + input.getValor());
            context.getLogger().log("Comentarios: " + input.getComentarios());

        }catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }

        String categoria_Pre_Procesamiento = input.getCategoría();
        String cuentas_Pre_Procesamiento = input.getCuenta();

        String categoria_Post_Procesamiento = Categorias.get(categoria_Pre_Procesamiento);
        String cuentas_Post_Procesamiento = Cuentas.get(cuentas_Pre_Procesamiento);

        Output output = new Output(input.getConcepto(),cuentas_Post_Procesamiento,categoria_Post_Procesamiento,input.getValor(),input.getComentarios());
        String jsonBody = "";
        try {
            jsonBody = buildNotionPayload(input,categoria_Post_Procesamiento,cuentas_Post_Procesamiento);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.notion.com/v1/pages"))
                    .header("Authorization", "Bearer " + NOTION_TOKEN)
                    .header("Notion-Version", "2026-03-11")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            context.getLogger().log("Notion respondio" + response.statusCode() + " - " + response.body());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Error de Notion: " + response.statusCode() + " - " + response.body());
            }
        }catch (Exception e){
            context.getLogger().log("Error consumiendo Notion: " + e.getMessage());
            throw new RuntimeException("Fallo al crear página en Notion", e);
        }


        return jsonBody;

    }
    private String buildNotionPayload(Input input, String catID, String cuentaID) throws JsonProcessingException {
        LocalDate fechaActual = LocalDate.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String fechaFormateada = fechaActual.format(formato);
        System.out.println(fechaFormateada);


        ObjectNode root = mapper.createObjectNode();
        ObjectNode parent = mapper.createObjectNode();
        parent.put("data_source_id", DATA_SOURCE_ID);
        root.set("parent",parent);
        ObjectNode properties = mapper.createObjectNode();


        // Concepto
        ObjectNode Concepto = mapper.createObjectNode();
        ArrayNode title = mapper.createArrayNode();
        ObjectNode text = mapper.createObjectNode();
        ObjectNode content = mapper.createObjectNode();
        content.put("content",input.getConcepto());
        text.set("text",content);
        title.add(text);
        Concepto.set("title",title);
        properties.set("Concepto",Concepto);
        // Fecha
        ObjectNode date = mapper.createObjectNode();
        ObjectNode start = mapper.createObjectNode();
        start.put("start", fechaFormateada);
        date.set("date",start);
        properties.set("Fecha",date);
        // Valor egresos
        ObjectNode number = mapper.createObjectNode();
        number.put("number",input.getValor());
        properties.set("Valor Egresos", number);
        // Comentarios
        ObjectNode Comentarios = mapper.createObjectNode();
        ArrayNode rich_text = mapper.createArrayNode();
        ObjectNode text_1 = mapper.createObjectNode();
        ObjectNode content_1 = mapper.createObjectNode();
        content_1.put("content",input.getComentarios());
        text_1.set("text", content_1);
        rich_text.add(text_1);
        Comentarios.set("rich_text",rich_text);
        properties.set("Comentarios", Comentarios);
        // Categoria
        ObjectNode cat = mapper.createObjectNode();
        ArrayNode relation = mapper.createArrayNode();
        ObjectNode id_cat = mapper.createObjectNode();
        id_cat.put("id", catID);
        relation.add(id_cat);
        cat.put("id", "TVyo");
        cat.put("type", "relation");
        cat.set("relation", relation);
        cat.put("has_more", false);
        properties.set("Categoría Egresos", cat);
        // Cuenta
        ObjectNode cuenta = mapper.createObjectNode();
        ArrayNode relation_1 = mapper.createArrayNode();
        ObjectNode id_cuenta = mapper.createObjectNode();
        id_cuenta.put("id",cuentaID);
        relation_1.add(id_cuenta);
        cuenta.put("id","TVyo");
        cuenta.put("type","relation");
        cuenta.set("relation",relation_1);
        cuenta.put("has_more",false);
        properties.set("Cuenta",cuenta);



        root.set("properties",properties);





        return mapper.writeValueAsString(root);
    }
}

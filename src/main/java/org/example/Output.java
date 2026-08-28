package org.example;

import java.util.UUID;

public class Output {
    private String Concepto;
    private String Cuenta;
    private String Categoría;
    private int valor;
    private String Comentartios;


    public Output(String concepto, String cuenta, String categoría, int valor, String comentartios) {
        Concepto = concepto;
        Cuenta = cuenta;
        Categoría = categoría;
        this.valor = valor;
        Comentartios = comentartios;
    }

    public String getConcepto() {
        return Concepto;
    }

    public void setConcepto(String concepto) {
        Concepto = concepto;
    }

    public String getComentartios() {
        return Comentartios;
    }

    public void setComentartios(String comentartios) {
        Comentartios = comentartios;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public String getCategoría() {
        return Categoría;
    }

    public void setCategoría(String categoría) {
        Categoría = categoría;
    }

    public String getCuenta() {
        return Cuenta;
    }

    public void setCuenta(String cuenta) {
        Cuenta = cuenta;
    }
}

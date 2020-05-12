package br.com.andre.carvalho.gae_project.exception;

public class ProductNotFoundException extends Exception {
    private String message;

    public ProductNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
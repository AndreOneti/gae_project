package br.com.andre.carvalho.gae_project.exception;

public class ProductInterestNotFoundException extends Exception {
    private String message;

    public ProductInterestNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

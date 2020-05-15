package br.com.andre.carvalho.gae_project.exception;

public class ProductAlreadyExistException extends Exception {
    private String message;

    public ProductAlreadyExistException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

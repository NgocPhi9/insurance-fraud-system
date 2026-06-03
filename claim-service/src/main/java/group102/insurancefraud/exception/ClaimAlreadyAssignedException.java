package group102.insurancefraud.exception;

public class ClaimAlreadyAssignedException extends RuntimeException {
  public ClaimAlreadyAssignedException(String message) {
    super(message);
  }
}

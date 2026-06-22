package group102.insurancefraud.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrainDetailDTO {

    private static final DateTimeFormatter API_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @JsonProperty("version")
    private String version;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("model_directory")
    private String modelDirectory;

    @JsonProperty("roc_curves_url")
    private String rocCurvesUrl;

    @JsonProperty("pr_curves_url")
    private String prCurvesUrl;

    @JsonProperty("confusion_matrices_url")
    private String confusionMatricesUrl;

    @JsonProperty("score_distributions_url")
    private String scoreDistributionsUrl;

    public String getDisplayTimestamp() {
        if (timestamp == null || timestamp.isBlank()) {
            return "";
        }

        try {
            return LocalDateTime.parse(timestamp, API_TIMESTAMP_FORMAT).format(DISPLAY_TIMESTAMP_FORMAT);
        } catch (DateTimeParseException e) {
            return timestamp;
        }
    }
}

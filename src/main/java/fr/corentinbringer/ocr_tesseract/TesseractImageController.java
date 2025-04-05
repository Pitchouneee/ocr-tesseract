package fr.corentinbringer.ocr_tesseract;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class TesseractImageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) throws IOException, TesseractException {
        // Convert to temporary file for Tesseract
        File tempImage = File.createTempFile("upload", ".jpg");
        try (FileOutputStream fos = new FileOutputStream(tempImage)) {
            fos.write(file.getBytes());
        }

        String extractedText = extractTextWithTesseract(tempImage);
        String response = callGemma(extractedText);

        model.addAttribute("json", response);
        return "index";
    }

    private String extractTextWithTesseract(File imageFile) throws TesseractException {
        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
        tesseract.setLanguage("fra");

        return tesseract.doOCR(imageFile);
    }

    private String callGemma(String extractedText) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", """
                        Analyze the following text and present it as structured JSON. Use keys and nested objects if applicable.
                        Only return the JSON, no explanation.

                        TEXT:
                        """ + extractedText
        );

        Map<String, Object> request = Map.of(
                "model", "gemma3:12b",
                "messages", List.of(message),
                "stream", false
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:11434/api/chat", entity, Map.class);

        Map<?, ?> responseMessage = (Map<?, ?>) response.getBody().get("message");
        return responseMessage.get("content").toString();
    }
}

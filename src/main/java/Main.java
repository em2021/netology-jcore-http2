import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Main {

    private static final String KEY = "z3H68x5dFphHAEwxn4HwzXlSpGq4x7NpdZk16hkI";
    private static final String REMOTE_SERVICE_URI = "https://api.nasa.gov/planetary/apod?api_key=" + KEY;
    public static ObjectMapper mapper = new ObjectMapper();

    public static String getMediaPath(String uriString) throws URISyntaxException {
        URI uri = new URI(uriString);
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static void main(String[] args) {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(5000)
                        .setSocketTimeout(30000)
                        .setRedirectsEnabled(false)
                        .build())
                .build();
        HttpGet request = new HttpGet(REMOTE_SERVICE_URI);
        request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            Post post = mapper.readValue(response.getEntity().getContent(), new TypeReference<Post>() {
            });
            String hdUrl = post.getHdurl();
            final String outputFilename;
            if (post.getMedia_type().equals("video")) {
                final String videoId = getMediaPath(hdUrl);
                hdUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                outputFilename = videoId + ".jpg";
            } else {
                outputFilename = getMediaPath(hdUrl);
            }
            request = new HttpGet(hdUrl);
            request.setHeader(HttpHeaders.ACCEPT, ContentType.IMAGE_JPEG.getMimeType());
            CloseableHttpResponse responseMedia = httpClient.execute(request);
            File outDir = new File("result");
            if (outDir.mkdir()) {
                System.out.printf("\'%s\' directory created successfully%n", outDir.getName());
            }
            byte[] bytes = responseMedia.getEntity().getContent().readAllBytes();
            try (FileOutputStream fos = new FileOutputStream(new File(outDir, outputFilename), false)) {
                fos.write(bytes);
                System.out.printf("\'%s\' file created successfully%n", outputFilename);
                fos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            response.close();
            httpClient.close();
            responseMedia.close();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
package com.aiassistant.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PdfIngestionService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    // filename -> list of text chunks
    private final Map<String, List<String>> pdfChunks = new ConcurrentHashMap<>();

    public String ingestPdf(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);
            List<String> chunks = splitIntoChunks(fullText);
            pdfChunks.put(filename, chunks);
            return String.format("Ingested '%s': %d chunks from %d pages", filename, chunks.size(), document.getNumberOfPages());
        }
    }

    public String getRelevantContext(String query, int topK) {
        if (pdfChunks.isEmpty()) {
            return "";
        }

        String queryLower = query.toLowerCase();
        String[] queryWords = queryLower.split("\\s+");

        List<ScoredChunk> scored = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : pdfChunks.entrySet()) {
            for (String chunk : entry.getValue()) {
                String chunkLower = chunk.toLowerCase();
                int score = 0;
                for (String word : queryWords) {
                    if (word.length() > 3 && chunkLower.contains(word)) {
                        score++;
                    }
                }
                if (score > 0) {
                    scored.add(new ScoredChunk(chunk, score, entry.getKey()));
                }
            }
        }

        scored.sort((a, b) -> b.score - a.score);

        StringBuilder context = new StringBuilder();
        int count = Math.min(topK, scored.size());
        for (int i = 0; i < count; i++) {
            ScoredChunk sc = scored.get(i);
            context.append("[From: ").append(sc.filename).append("]\n");
            context.append(sc.chunk).append("\n\n");
        }

        return context.toString().trim();
    }

    public List<String> listUploadedFiles() {
        return new ArrayList<>(pdfChunks.keySet());
    }

    public void removeFile(String filename) {
        pdfChunks.remove(filename);
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        String cleaned = text.replaceAll("\\s+", " ").trim();
        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + CHUNK_SIZE, cleaned.length());
            // try to break at a sentence boundary
            if (end < cleaned.length()) {
                int lastPeriod = cleaned.lastIndexOf('.', end);
                if (lastPeriod > start + CHUNK_SIZE / 2) {
                    end = lastPeriod + 1;
                }
            }
            chunks.add(cleaned.substring(start, end).trim());
            start = end - CHUNK_OVERLAP;
            if (start >= cleaned.length() - CHUNK_OVERLAP) break;
        }
        return chunks;
    }

    private record ScoredChunk(String chunk, int score, String filename) {}
}

package com.careup.branch.domain.chat.service;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 텍스트를 '라인' 단위로 그룹화하여 시각적 순서에 맞게 정렬하는 Stripper
 */
public class PositionAwarePDFTextStripper extends PDFTextStripper {

    private final List<TextLine> lines = new ArrayList<>();

    public PositionAwarePDFTextStripper() throws IOException {
        super();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (textPositions.isEmpty() || text.trim().isEmpty()) {
            return;
        }
        TextPosition firstPos = textPositions.get(0);
        TextChunk newChunk = new TextChunk(text.trim(), firstPos.getXDirAdj(), firstPos.getYDirAdj());
        findAndAddChunkToLine(newChunk);
    }

    private void findAndAddChunkToLine(TextChunk newChunk) {
        for (TextLine line : lines) {
            if (line.isSameLine(newChunk)) {
                line.addChunk(newChunk);
                return;
            }
        }
        TextLine newLine = new TextLine();
        newLine.addChunk(newChunk);
        lines.add(newLine);
    }

    public String getSortedText() {
        lines.sort(Comparator.comparing(TextLine::getAverageY));
        StringBuilder result = new StringBuilder();
        for (TextLine line : lines) {
            result.append(line.getMergedText()).append("\n");
        }
        return result.toString();
    }

    // 텍스트 조각
    private static class TextChunk {
        final String text;
        final float x;
        final float y;
        TextChunk(String text, float x, float y) { this.text = text; this.x = x; this.y = y; }
    }

    // 텍스트 라인
    private static class TextLine {
        private final List<TextChunk> chunks = new ArrayList<>();
        private float totalY = 0;

        public void addChunk(TextChunk chunk) {
            chunks.add(chunk);
            totalY += chunk.y;
        }

        public float getAverageY() { return chunks.isEmpty() ? 0 : totalY / chunks.size(); }

        public boolean isSameLine(TextChunk chunk) {
            return chunks.isEmpty() || Math.abs(chunk.y - getAverageY()) < 5.0f; // y좌표 허용 오차를 5.0f로 넉넉하게 설정
        }

        // 지능적 병합 로직
        public String getMergedText() {
            if (chunks.isEmpty()) return "";
            chunks.sort(Comparator.comparing(c -> c.x));

            String template = null; // 양식 템플릿 (예: "산정 기준 : 시간급제 원/시간")
            String value = null;    // 값 (예: "9620")

            StringBuilder lineBuilder = new StringBuilder();
            for (TextChunk chunk : chunks) {
                // 숫자로만 구성된 청크를 '값'으로 추정
                if (chunk.text.matches("\\d+")) {
                    value = chunk.text;
                } else {
                    lineBuilder.append(chunk.text).append(" ");
                }
            }
            template = lineBuilder.toString().trim();

            // 값과 템플릿이 모두 존재하고, 템플릿에 값을 삽입할 여지가 있는 경우
            if (value != null && template != null && template.contains("시간급제") && template.contains("원/시간")) {
                // "원/시간" 앞에 값을 삽입
                return template.replace("원/시간", value + " 원/시간");
            }

            // 위의 특수 케이스에 해당하지 않으면, 그냥 순서대로 합침
            StringBuilder defaultBuilder = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                defaultBuilder.append(chunks.get(i).text);
                if (i < chunks.size() - 1) {
                    defaultBuilder.append(" ");
                }
            }
            return defaultBuilder.toString();
        }
    }
}
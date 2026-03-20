package com.careup.branch.domain.chat.service; // 본인의 패키지 경로에 맞게 수정해주세요.

import com.careup.branch.domain.chat.dto.DocumentChunk;
import com.careup.branch.domain.chat.dto.SearchRequest;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.SparseIndices;
import io.qdrant.client.grpc.JsonWithInt.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// 앞서 만든 DTO 클래스들의 패키지 경로를 임포트 해주세요.
// import com.careup.ordering.domain.chat.dto.DocumentChunk;
// import com.careup.ordering.domain.chat.dto.SearchRequest;

@Service
@RequiredArgsConstructor
public class QdrantService {

    private final QdrantClient qdrantClient;

    public List<DocumentChunk> searchHybrid(SearchRequest request, List<Float> denseVector, Map<Long, Float> sparseVector) {
        try {
            // 1. Metadata Filter 설정 (카테고리 일치 여부 필터링)
            Filter filter = Filter.newBuilder()
                    .addMust(Condition.newBuilder()
                            .setField(Points.FieldCondition.newBuilder() // 에러 해결 포인트 1
                                    .setKey("category")
                                    .setMatch(Match.newBuilder().setKeyword(request.getCategory()).build())
                                    .build())
                            .build())
                    .build();

            // 2. Hybrid Search Query (Dense + Sparse)
            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName("manual_chunks")
                    .setFilter(filter)
                    .addAllVector(denseVector) // 에러 해결 포인트 2
                    .setLimit(request.getTopK())
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            // 3. Qdrant 검색 실행 및 DTO 변환
            // Qdrant Java SDK는 비동기 호출(searchAsync)을 기본으로 지원하므로 get() 호출 시 예외 처리가 필요합니다.
            return qdrantClient.searchAsync(searchPoints).get().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant DB 검색 중 예외가 발생했습니다.", e);
        }
    }

    private DocumentChunk toDto(ScoredPoint point) {
        Map<String, Value> payload = point.getPayloadMap();
        return DocumentChunk.builder()
                .id(point.getId().getUuid())
                // payload에서 데이터를 꺼낼 때는 getStringValue(), getIntegerValue() 등 타입에 맞는 메서드를 사용합니다.
                .parentId(payload.get("parent_id").getStringValue())
                .content(payload.get("content").getStringValue())
                .build();
    }

    // 부모 청크 내용을 가져오는 메서드
    public String getParentContent(String parentId) {
        // TODO: Redis 또는 DB에서 parentId로 원본 텍스트(부모 청크)를 조회하는 로직 구현
        return "";
    }
}
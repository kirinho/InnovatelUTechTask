package com.liushukov;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {


    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();

    /**
     * Implementation of this method should upsert the document to your storage
     * And generate unique id if it does not exist, don't change [created] field
     *
     * @param document - document content and author data
     * @return saved document
     */
    public Document save(Document document) {

        if (document.getId() == null || document.getId().isEmpty()) {
            document.setId(UUID.randomUUID().toString());
        } else {
            var existedDocument = findById(document.getId());
            existedDocument.ifPresent(value -> document.setCreated(value.getCreated()));
        }
        documentStore.put(document.getId(), document);
        return document;
    }

    /**
     * Implementation this method should find documents which match with request
     *
     * @param request - search request, each field could be null
     * @return list matched documents
     */
    public List<Document> search(SearchRequest request) {
        return documentStore.values().stream()
                .filter(document -> filter(document, request.getTitlePrefixes(), Filter.TITLE))
                .filter(document -> filter(document, request.getContainsContents(), Filter.CONTENT))
                .filter(document -> filter(document, request.getAuthorIds(), Filter.AUTHOR))
                .filter(document -> filterByTime(document, request.getCreatedFrom(), request.getCreatedTo()))
                .toList();
    }

    private boolean filter(Document document, List<String> list, Filter filter) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return switch (filter) {
            case TITLE -> list.stream().anyMatch(prefix -> document.getTitle().toLowerCase().startsWith(prefix.toLowerCase()));
            case CONTENT -> list.stream().anyMatch(content -> document.getContent().toLowerCase().contains(content.toLowerCase()));
            case AUTHOR -> list.contains(document.getAuthor().getId());
        };
    }

    private boolean filterByTime(Document document, Instant createdFrom, Instant createdTo) {
        var createdDocument = document.getCreated();
        return (createdFrom == null || !createdDocument.isBefore(createdFrom)) &&
                (createdTo == null || !createdDocument.isAfter(createdTo));
    }

    /**
     * Implementation this method should find document by id
     *
     * @param id - document id
     * @return optional document
     */
    public Optional<Document> findById(String id) {

        return Optional.ofNullable(documentStore.get(id));
    }

    @Data
    @Builder
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }

    @Data
    @Builder
    public static class Document {
        private String id;
        private String title;
        private String content;
        private Author author;
        private Instant created;
    }

    @Data
    @Builder
    public static class Author {
        private String id;
        private String name;
    }

    public enum Filter {
        TITLE,
        CONTENT,
        AUTHOR;
    }
}

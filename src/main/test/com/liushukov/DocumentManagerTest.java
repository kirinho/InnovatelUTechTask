package com.liushukov;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentManagerTest {

    private DocumentManager documentManager;
    private DocumentManager.Author author;

    private String documentId;
    private String documentTitle;
    private String documentContent;

    private Instant documentCreated;


    @BeforeEach
    public void setUp() {
        documentManager = new DocumentManager();

        var authorId = UUID.randomUUID().toString();
        var authorName = "Test name";
        author = DocumentManager.Author.builder()
                .id(authorId)
                .name(authorName)
                .build();

        documentTitle = "Test document title";
        documentContent = "Test document content";
        documentId = UUID.randomUUID().toString();
        documentCreated = Instant.now();
        var document1 = DocumentManager.Document.builder()
                .id(documentId)
                .title(documentTitle)
                .content(documentContent)
                .author(author)
                .created(documentCreated)
                .build();

        var document2 = DocumentManager.Document.builder()
                .id(UUID.randomUUID().toString())
                .title("blah " + documentTitle)
                .content(documentContent + " blah")
                .author(author)
                .created(documentCreated.plusSeconds(10))
                .build();

        var document3 = DocumentManager.Document.builder()
                .id(UUID.randomUUID().toString())
                .title("blah" + documentTitle)
                .content("blah" + documentContent)
                .author(author)
                .created(documentCreated.plusSeconds(15))
                .build();

        documentManager.save(document1);
        documentManager.save(document2);
        documentManager.save(document3);
    }

    @Test
    public void testSaveNewDocumentWithoutId() {

        var document = DocumentManager.Document.builder()
                .title(documentTitle)
                .content(documentContent)
                .author(author)
                .created(Instant.now())
                .build();

        var savedDocument = documentManager.save(document);

        assertNotNull(savedDocument.getId(), "new document should have generated id");
        var documentFromStorage = documentManager.findById(savedDocument.getId());
        assertTrue(documentFromStorage.isPresent(), "document should be in storage");
        assertEquals(document, documentFromStorage.get(), "documents should be equals");
    }

    @Test
    public void testSaveDocumentWithExistedId() {
        var doc1 = documentManager.findById(documentId);
        var documentId1 = doc1.isPresent() ? doc1.get().getId() : UUID.randomUUID().toString();
        var document2 = DocumentManager.Document.builder()
                .id(documentId1)
                .title(documentTitle + " blah")
                .content(documentContent)
                .author(author)
                .created(documentCreated.plusSeconds(5))
                .build();

        var savedDocument2 = documentManager.save(document2);

        assertEquals(documentId, savedDocument2.getId(), "ids should be equals");
        assertEquals(documentCreated, savedDocument2.getCreated(),
                "created time shouldn't be changed");
        assertNotEquals(documentTitle, savedDocument2.getTitle(),
                "if was modified an attribute it should be changed");
        assertEquals(documentContent, savedDocument2.getContent(),
                "if wasn't modified an attribute it should be equals");
    }

    @Test
    public void testFindByIdDocument() {
        var documentFromStorage = documentManager.findById(documentId);
        assertTrue(documentFromStorage.isPresent(), "document should be in storage");

        var documentFromNotStorage = documentManager.findById("12345678");
        assertFalse(documentFromNotStorage.isPresent(), "document shouldn't be in storage");
    }

    @Test
    public void testSearchWithNoFilters() {
        var response = documentManager.search(DocumentManager.SearchRequest.builder().build());
        assertEquals(3, response.size(), "should return all docs and size of collection must be 3");
    }

    @Test
    public void testSearchTitlePrefixes() {
        var searchRequest = DocumentManager.SearchRequest.builder().titlePrefixes(List.of("t", "ar", "li")).build();
        var response = documentManager.search(searchRequest);

        assertEquals(1, response.size(),"should return list with 1 element");
        assertEquals(documentId, response.get(0).getId(), "ids should be equals");
    }

    @Test
    public void testSearchContainsContent() {
        var searchRequest = DocumentManager.SearchRequest.builder().containsContents(List.of("blah", "element")).build();
        var response = documentManager.search(searchRequest);

        assertEquals(2, response.size(), "should return list with 2 elements");
    }

    @Test
    public void testSearchContainsAuthorId() {
        var searchRequest = DocumentManager.SearchRequest.builder().authorIds(List.of(author.getId(), "12345678")).build();
        var response = documentManager.search(searchRequest);

        assertEquals(3, response.size(), "should return list with 3 elements");
    }

    @Test
    public void testSearchByTime() {
        var searchRequest = DocumentManager.SearchRequest.builder()
                .createdFrom(documentCreated.minusSeconds(3))
                .createdTo(documentCreated.plusSeconds(3))
                .build();

        var response = documentManager.search(searchRequest);
        assertEquals(1, response.size(), "should return list with 1 element");
        assertEquals(documentCreated, response.get(0).getCreated(), "created time should be equals");
    }

    @Test
    public void testAllSearchConditions() {
        var searchRequest = DocumentManager.SearchRequest.builder()
                .titlePrefixes(List.of("blah", "oh", "re", "t"))
                .containsContents(List.of("blah", "test"))
                .authorIds(List.of(author.getId(), "987654321"))
                .createdFrom(documentCreated.plusSeconds(1))
                .createdTo(documentCreated.plusSeconds(11))
                .build();

        var response = documentManager.search(searchRequest);
        assertEquals(1, response.size(), "should return list with 1 element (document 2)");
        assertEquals(documentCreated.plusSeconds(10), response.get(0).getCreated(), "created time should be equals");
    }
}

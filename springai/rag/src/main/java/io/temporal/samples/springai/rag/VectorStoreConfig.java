package io.temporal.samples.springai.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the vector store.
 *
 * <p>This example uses Spring AI's SimpleVectorStore, an in-memory vector store that's perfect for
 * demos and testing. In production, you'd use a real vector database like Pinecone, Weaviate,
 * Milvus, or pgvector.
 */
@Configuration
public class VectorStoreConfig {

  /**
   * Creates an in-memory vector store using the provided embedding model.
   *
   * <p>The SimpleVectorStore stores vectors in memory and uses the embedding model to convert text
   * to vectors when documents are added.
   *
   * @param embeddingModel the embedding model to use for vectorization
   * @return the configured vector store
   */
  @Bean
  public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return SimpleVectorStore.builder(embeddingModel).build();
  }
}

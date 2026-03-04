-- Enable the pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the vector_store table
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(768)
);

-- Create HNSW index
CREATE INDEX ON vector_store USING hnsw (embedding vector_cosine_ops);

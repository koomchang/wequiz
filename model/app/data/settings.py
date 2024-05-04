# # preprocessor.py
BUCKET_NAME = 'wequiz-files'
PORT = 9010
PARSING_RETRY = 3

# keyword chunk
KEYWORD_CHUNK_SIZE = 4000
KEYWORD_CHUNK_OVERLAP = 0

# summary chunk
SUMMARY_CHUNK_SIZE = 3000
SUMMARY_CHUNK_OVERLAP = 600

# vector chunk
VECTOR_CHUNK_SIZE = 300
VECTOR_CHUNK_OVERLAP = 30


# generator.py
QUIZ_GENERATE_RETRY = 3
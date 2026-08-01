from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    email: str  #Don't forget to add ur email and password to a .env file
    password: str
    openrouter_key: str = "" # Added for logical centralized AI calls

    class Config:
        env_file = ".env"  # Name of the environment file to load
        env_file_encoding = "utf-8"  # Encoding used in the .env file
        case_sensitive = False  # Make environment variable names case-insensitive

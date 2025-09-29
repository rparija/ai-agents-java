# Environment Variable Setup for LLM1

To use the `LLM1` class in this project, you must set the following environment variables to enable access to the Hugging Face API:

## Required Variables

- **HUGGINGFACE_API_KEY** or **HF_TOKEN**: Your Hugging Face API key or token. At least one of these must be set. If both are set, `HUGGINGFACE_API_KEY` takes precedence.
- **HF_MODEL_ID** (optional): The model ID to use. If not set, defaults to `openai/gpt-oss-20b:novita`.
- **HF_BASE_URL** (optional): The base URL for the Hugging Face inference endpoint. If not set, defaults to `https://router.huggingface.co/v1/chat/completions`.

## How to Set Environment Variables

### On Windows (Command Prompt)
```
set HUGGINGFACE_API_KEY=your_huggingface_api_key
set HF_MODEL_ID=your_model_id
set HF_BASE_URL=your_base_url
```

### On Windows (PowerShell)
```
$env:HUGGINGFACE_API_KEY="your_huggingface_api_key"
$env:HF_MODEL_ID="your_model_id"
$env:HF_BASE_URL="your_base_url"
```

### On Linux/macOS (Bash)
```
export HUGGINGFACE_API_KEY=your_huggingface_api_key
export HF_MODEL_ID=your_model_id
export HF_BASE_URL=your_base_url
```

## Example
```
set HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxxxxxxxxxxxxx
set HF_MODEL_ID=openai/gpt-oss-20b:novita
```

## Notes
- If you do not set `HUGGINGFACE_API_KEY` or `HF_TOKEN`, the application will throw an error and not run.
- You can add these variables to your system environment variables for persistence, or set them in your shell before running the application.
- For more information on obtaining a Hugging Face API key, visit: https://huggingface.co/settings/tokens


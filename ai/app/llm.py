from typing import Protocol

import httpx


class LlmConfigurationError(RuntimeError):
    pass


class LlmTimeoutError(RuntimeError):
    pass


class LlmProviderError(RuntimeError):
    pass


class LanguageModel(Protocol):
    def generate(self, instructions: str, input_text: str) -> str:
        """Generate a text response from the supplied instructions and input."""


class OpenAIResponsesClient:
    def __init__(
        self,
        api_key: str | None,
        model: str,
        timeout_seconds: float,
        max_output_tokens: int,
    ) -> None:
        self.api_key = api_key
        self.model = model
        self.timeout_seconds = timeout_seconds
        self.max_output_tokens = max_output_tokens

    def generate(self, instructions: str, input_text: str) -> str:
        if not self.api_key:
            raise LlmConfigurationError("OPENAI_API_KEY is not configured.")
        try:
            response = httpx.post(
                "https://api.openai.com/v1/responses",
                headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
                json={
                    "model": self.model,
                    "instructions": instructions,
                    "input": input_text,
                    "max_output_tokens": self.max_output_tokens,
                },
                timeout=self.timeout_seconds,
            )
            response.raise_for_status()
        except httpx.TimeoutException as exc:
            raise LlmTimeoutError("The language model request timed out.") from exc
        except httpx.HTTPError as exc:
            raise LlmProviderError("The language model request failed.") from exc

        try:
            answer = self._extract_output_text(response.json())
        except ValueError as exc:
            raise LlmProviderError("The language model returned invalid JSON.") from exc
        if not answer:
            raise LlmProviderError("The language model returned no text.")
        return answer

    @staticmethod
    def _extract_output_text(payload: dict) -> str:
        texts = []
        for item in payload.get("output", []):
            if item.get("type") != "message":
                continue
            for content in item.get("content", []):
                if content.get("type") == "output_text" and content.get("text"):
                    texts.append(content["text"])
        return "\n".join(texts).strip()

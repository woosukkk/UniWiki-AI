FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PYTHONPATH=/app
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends gosu \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app uniwiki
COPY requirements.txt ./
RUN pip install --upgrade pip \
    && pip install -r requirements.txt
COPY app app
COPY tests tests
COPY scripts scripts
COPY data/normalized data/normalized
COPY data/raw data/raw
RUN pytest -q
COPY docker-entrypoint.sh /usr/local/bin/uniwiki-ai-entrypoint
RUN mkdir -p /data/chroma && chown -R 10001:10001 /app /data
RUN sed -i 's/\r$//' /usr/local/bin/uniwiki-ai-entrypoint \
    && chmod +x /usr/local/bin/uniwiki-ai-entrypoint
EXPOSE 8000
ENTRYPOINT ["/usr/local/bin/uniwiki-ai-entrypoint"]

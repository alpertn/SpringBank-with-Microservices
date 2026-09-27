FROM python:3.12-slim-bookworm AS builder

ARG LIBPOSTAL_COMMIT=25099c506612b34b23b1bfe286ca6321fcf06f35
ARG PYPOSTAL_COMMIT=d6666a4f6a2ae0e7b83e037a35412f0f6b45c318
RUN apt-get update && apt-get install -y --no-install-recommends \
    autoconf automake build-essential curl git libtool pkg-config python3-dev && \
    rm -rf /var/lib/apt/lists/*
RUN git clone https://github.com/openvenues/libpostal.git /src/libpostal && \
    cd /src/libpostal && git checkout ${LIBPOSTAL_COMMIT} && ./bootstrap.sh && \
    ./configure --datadir=/opt/libpostal-data && make -j"$(nproc)" && make install && ldconfig
RUN pip install --no-cache-dir "git+https://github.com/openvenues/pypostal.git@${PYPOSTAL_COMMIT}"

FROM python:3.12-slim-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends curl libgomp1 && rm -rf /var/lib/apt/lists/* && \
    useradd --system --uid 10001 --create-home app
COPY --from=builder /usr/local /usr/local
COPY --from=builder /opt/libpostal-data /opt/libpostal-data
COPY requirements.txt /tmp/requirements.txt
RUN ldconfig && pip install --no-cache-dir -r /tmp/requirements.txt
WORKDIR /app
COPY app /app
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/health/ready || exit 1
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080", "--workers", "1"]

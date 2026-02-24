# 📊 How to See Monitoring

Monitoring is now **fully automated**. Prometheus and Grafana will start and stop automatically when you run the application.

## 1. Automatic Local Setup
You no longer need to run `docker-compose` manually.

### Prerequisites
- Docker (run the Docker Desktop application)
- Run the Attendance Application normally.

### Steps
1. **Start the Application**:
   Run:
   ```bash
   ./mvnw spring-boot:run
   ```
   Spring Boot will detect the `docker-compose.yml` file and launch the monitoring containers automatically.
2. **Watch the Console**:
   On startup, you will see a banner with the URLs:
   ```text
   🚀 Monitoring Stack is available!
   📊 Prometheus: http://localhost:9090
   📈 Grafana:    http://localhost:3000
   ```
3. **Login to Grafana**:
   Open [http://localhost:3000](http://localhost:3000) (Login: `admin` / `admin`).
   - **Common Fix for "No data":** Ensure the time range in the top right of Grafana is set to **"Last 5 minutes"** and that you have clicked around the application pages to generate some metrics.
4. **Import Dashboard**:
   - Go to `Dashboards -> New -> Import`.
   - Use Dashboard ID: **4701** (JVM Micrometer dashboard).
   - Select **Prometheus** as the data source after it loads the JSON.

---

## 🛠️ Troubleshooting "No Data"
If graphs are still blank:
1.  **Metric generation**: Visit [http://localhost:8081/actuator/prometheus](http://localhost:8081/actuator/prometheus) in your browser. If you see text, metrics are working.
2.  **Scrape Check**: Visit [http://localhost:9090/targets](http://localhost:9090/targets). `attendance-app` must be **UP**.
3.  **Grafana Sync**: Refresh Grafana after generating some HTTP requests (like refreshing the dashboard or logging in).
4.  **Time range**: Check the top-right corner of Grafana and set it to **Last 5 minutes**.

## 2. Kubernetes Setup (Production-like)
This uses your EKS cluster.

### Steps
1. **Apply Monitoring Stack**:
   ```bash
   kubectl apply -f k8s/monitoring.yaml
   ```
2. **Access Services**:
   Find the external URLs for the LoadBalancers:
   ```bash
   kubectl get svc -n monitoring
   ```
3. **Login**:
   Grafana is pre-configured with a Prometheus datasource pointing to the internal service.

---

## 🛠️ Metrics to Look For
- `http_server_requests_seconds_count`: Number of HTTP requests.
- `jvm_memory_used_bytes`: Memory usage of the Java application.
- `process_uptime_seconds`: How long the app has been running.
- `logback_events_total`: Total number of log events (errors/warnings).

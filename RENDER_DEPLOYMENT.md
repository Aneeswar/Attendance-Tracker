# Render Deployment Guide

## What is Render?
Render is a modern cloud platform that automatically deploys your app from GitHub. It has:
- Free tier with 750 free hours/month per service
- Auto-deploys on every GitHub push
- Free PostgreSQL database included
- Built-in SSL/HTTPS
- Simple configuration

## Prerequisites
1. GitHub account with your code pushed
2. Render account (free - sign up at render.com)
3. PostgreSQL database (Render provides free tier)

## Step 1: Push Code to GitHub

```bash
# Initialize git (if not already done)
git init
git add .
git commit -m "Add Render deployment configuration"
git branch -M main

# Add your GitHub remote
git remote add origin https://github.com/YOUR_USERNAME/attendance-app.git
git push -u origin main
```

## Step 2: Create Render Account

1. Go to https://render.com
2. Sign up with GitHub (recommended - easier authentication)
3. Authorize Render to access your GitHub account

## Step 3: Deploy via render.yaml (Easiest)

The `render.yaml` file in the root directory automatically configures everything:

1. Go to https://dashboard.render.com/blueprint
2. Click "New Blueprint Instance"
3. Select your GitHub repository
4. Render will auto-detect `render.yaml` and set up:
   - **Web Service**: Your Spring Boot app
   - **PostgreSQL Database**: Automatically provisioned
   - **Environment Variables**: Database connection strings

4. Click "Create Blueprint Instance"
5. Wait for deployment to complete (3-5 minutes)

## Step 4: Manual Deployment (Alternative)

If you prefer manual setup:

1. **Create Web Service:**
   - Go to https://dashboard.render.com/services
   - Click "New +" → "Web Service"
   - Select your GitHub repo
   - Fill in:
     - **Name**: `attendance-app`
     - **Environment**: Java
     - **Build Command**: `mvn clean package -DskipTests`
     - **Start Command**: `java -jar target/Attendance-0.0.1-SNAPSHOT.jar`
     - **Plan**: Free
   - Click "Create Web Service"

2. **Create PostgreSQL Database:**
   - Go to https://dashboard.render.com/databases
   - Click "New +" → "PostgreSQL"
   - Fill in:
     - **Name**: `attendance-db`
     - **Database**: `attendance_db`
     - **User**: `postgres`
     - **Plan**: Free
   - Click "Create Database"

3. **Connect Database to Web Service:**
   - Go to your web service settings
   - Go to "Environment" tab
   - Add these environment variables:
     ```
     SPRING_DATASOURCE_URL=postgres://USER:PASSWORD@HOST:PORT/DATABASE
     SPRING_DATASOURCE_USERNAME=postgres
     SPRING_DATASOURCE_PASSWORD=PASSWORD
     PORT=10000
     JAVA_TOOL_OPTIONS=-Xmx256m
     ```
   - You'll find the connection string in the PostgreSQL database dashboard

4. **Manual Redeploy:**
   - Push changes to GitHub
   - Render automatically redeploys
   - Or click "Manual Deploy" in the dashboard

## Step 5: Configure Application Properties

Update `src/main/resources/application.properties` to use environment variables:

```properties
spring.application.name=Attendance

# Use environment variables or defaults
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/attendance_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:0000}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=my_super_secret_key_for_jwt_token_generation_that_is_long_enough_to_be_secure_and_meets_the_requirements_of_512bits_minimum_length_needed_for_HS512
jwt.expiration=86400000

# Server Configuration - Render sets this automatically
server.port=${PORT:8081}

# Logging Configuration
logging.level.root=INFO
logging.level.com.deepak.Attendance=DEBUG
```

## Step 6: View Logs & Monitor

```
In Render Dashboard:
- Click your web service
- Go to "Logs" tab to see real-time logs
- Go to "Metrics" tab to see CPU/Memory usage
- Go to "Events" tab to see deployment history
```

## Important Notes

### Free Tier Limitations
- **Web Service**: 750 free hours/month (runs continuously for ~25 days)
- **Database**: Free PostgreSQL with 100MB storage
- **Sleep**: Free services spin down after 15 minutes of inactivity (first request after spin-down takes 30-60 seconds)
- **Bandwidth**: 100GB/month included

### File Uploads
The `/uploads` directory won't persist between deployments. Options:
1. **Use Render Disk** (paid feature)
2. **Use external storage** (AWS S3, Google Cloud Storage)
3. **Disable upload feature** in free tier

### Cost Examples
- **Free tier**: Completely free if within 750 hours/month
- **Premium**: $7-12/month if you need no sleep periods or more resources

## Auto-Deploy on GitHub Push

Once connected, Render automatically deploys when you:
```bash
git push origin main
```

Watch deployment in real-time in the Render dashboard → "Logs" tab.

## Environment Variables & Secrets

**To add/update env vars:**
1. Go to Web Service → Environment
2. Add variables or update existing ones
3. Click "Save Configuration"
4. Service auto-redeploys with new config

**For sensitive data:**
- Use Render's encrypted environment variables
- Never commit secrets to GitHub

## Troubleshooting

**Issue: Service keeps spinning down**
- Free tier limitation - upgrade to paid or use render.com status page

**Issue: Database connection error**
- Check env vars in dashboard match database credentials
- Ensure PostgreSQL database is created
- Click database → "Connections" to verify credentials

**Issue: Build fails**
- Check "Build & Deploy" logs
- Ensure Java 21 is specified in render.yaml
- Verify pom.xml has all required dependencies

**Issue: 502 Bad Gateway**
- App may be starting up
- Check "Logs" for startup errors
- Ensure PORT environment variable is set

## Next Steps

1. Push to GitHub with render.yaml
2. Authorize Render to GitHub
3. Create blueprint instance or manual deployment
4. Visit your app at: `https://attendance-app.onrender.com`

---

**Your free app URL will be**: `https://YOUR_SERVICE_NAME.onrender.com`

All traffic is auto-encrypted with HTTPS included!

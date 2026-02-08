 Java Cat Bot - UrFU Project 

1. Create .env file with your bot token
echo "TELEGRAM_BOT_TOKEN= "TOKEN " > .env

2. Build Docker images
docker-compose build

3. Start all services
docker-compose up -d

start cat-bot =>

Start RabbitMQ container => docker-compose up -d rabbitmq

mvn clean install -DskipTests
export TELEGRAM_BOT_TOKEN="-"
java -jar telegram-adapter/target/telegram-adapter-1.0.0.jar


 Start services in separate terminals =>
  Terminal 1: Cat Service (business logic + DB)
java -jar cat-service/target/cat-service-1.0.0.jar

  Terminal 2: Telegram Adapter (user interface)
java -jar telegram-adapter/target/telegram-adapter-1.0.0.jar



A multilingual Telegram bot for cat adoption with microservice architecture, built for Ural Federal University (UrFU) course requirements.

 [Java 17] 
 [Spring Boot] 
 [Docker] 
 
 Project Requirements Compliance

 Requirement  & Implementation 

- Two independent services - `telegram-adapter` + `cat-service`  
- Inter-service communication - RabbitMQ message broker  
- Database persistence - H2 (dev) / PostgreSQL (prod)  
- SOLID principles - Clean architecture, no reverse dependencies  
- Multilingual support - Persian / English / Russian (i18n)  
- 8 real cat samples - With real Telegram `file_id` values  
- Reactions system - ❤️ 🔥 😲 👍 👎 with instant UI update  
- Comments system - Instant display after submission  
- Age validation - Strict numeric validation (1-300 months)  
- No hardcoded tokens - Environment variable `TELEGRAM_BOT_TOKEN`  
- Proper .gitignore - Excludes `target/`, `.DS_Store`, IDE files  
- Dockerized - Ready for containerized deployment  

  Features

- Trilingual interface (Persian/English/Russian) with language selection
- User-generated content: Add new cats with photo, name, color, breed, age (1-300 months), description
- Reaction system**: Instant emoji reactions (❤️ 🔥 😲 👍 👎) with real-time counter updates
- Comment system: Add comments to cats with instant display
- Navigation : Previous/Next buttons for browsing cats
- Input validation: Strict age validation (numbers only, 1-300 range)
- Auto-start : No `/start` command required - bot initializes on first message
- Clean UI: Single "Back" button to main menu (no confusing navigation)
 
 
 Technical Stack

- Core Technologies
- Java 17 - Language runtime
- Spring Boot 3.3.0 - Application framework
- Spring Data JPA - Database abstraction
- RabbitMQ - Async inter-service communication
- H2 Database - In-memory DB for development
- PostgreSQl - Production-ready relational DB
- Telegram Bot API - Messaging interface

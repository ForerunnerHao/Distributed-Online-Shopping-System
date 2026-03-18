# Setup the Store-App

## Base info

Database:
username: admin
password: admin

RabbitMQ:
username: admin
password: admin

PgAdmin:
username: admin@admin.com
password: admin

Frontend :5173
React

Port list:
store: 8080 database:5433 swagger:8080/docs pgadmin: 5051
warehouse: 8081 database:5433 pgadmin: 5051
warehouse: 8082 database:5433 pgadmin: 5051
DeliveryCo: 8084 database:5434 swagger:8084/docs pgadmin: 5054
EmailService:8085 database:5435 swagger:8085/docs pgadmin: 5055
BankApp:8086 database:5436 swagger:8086/docs pgadmin: 5056
RabbitMQ: 5672 & 15672

Base Background:
Springboot 3.3.2
Gradle 8.8
java 17

## Docker List

```dockerfile
  postgres-store:
    image: postgres:16-alpine
    container_name: postgres-store
    restart: unless-stopped
    environment:
      POSTGRES_DB: store

  postgres-bank:
    image: postgres:16-alpine
    container_name: postgres-bank
    restart: unless-stopped
    environment:
      POSTGRES_DB: bank

  postgres-delivery:
    image: postgres:16-alpine
    container_name: postgres-delivery
    restart: unless-stopped
    environment:
      POSTGRES_DB: delivery

  postgres-email:
    image: postgres:16-alpine
    container_name: postgres-email
    restart: unless-stopped
    environment:
      POSTGRES_DB: emaildb

  pgadmin:
    image: dpage/pgadmin4:8
    container_name: store_pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@admin.com
      PGADMIN_DEFAULT_PASSWORD: admin
    
  rabbitmq:
    image: rabbitmq:4-management
    container_name: rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin
```



## Setup Store & Warehouses(2)

1. go to `~\Tutorial-07-Group-08\Store`
2. Build (because proto)
3. Running `~\Tutorial-07-Group-08\Store\store-app\src\main\java\Tutorial7_8\Store\StoreApplication.java`
4. Running `~\Tutorial-07-Group-08\Store\warehouse\src\main\java\Tutorial7_8\warehouse01\Warehouse1Application.java`
5. Running `~\Tutorial-07-Group-08\Store\warehouse\src\main\java\Tutorial7_8\warehouse02\Warehouse2Application.java`

## Bank-App

1. go to `~\Tutorial-07-Group-08\3rdParty\BankApp`
2. Run `~\Tutorial-07-Group-08\3rdParty\BankApp\src\main\java\com\bank\BankApp\BankAppApplication.java`

## DeliveryCo

1. go to `~\Tutorial-07-Group-08\3rdParty\DeliveryCo`

2. Run `~\Tutorial-07-Group-08\3rdParty\BankApp\src\main\java\com\delivery\DeliveryCo\DeliveryCoApplication.java`

   

## Email Service

1. go to `~\Tutorial-07-Group-08\3rdParty\EmailService`
2. Run `~\Tutorial-07-Group-08\3rdParty\BankApp\src\main\java\com\email\EmailService\EmailServiceApplication.java`

## Frontend (React)

1. go to `~\Tutorial-07-Group-08\frontend`
2. execute `npm install`
3. execute `npm run dev`

Store API:
https://rni4xwmlb1.apifox.cn/getallitems-370020272e0

>  All Dockerfiles are corrupted
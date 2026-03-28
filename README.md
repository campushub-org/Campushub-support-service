# 📚 CampusHub - Support Service

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

> Le **Support Service** orchestre le cycle de vie du matériel pédagogique au sein de CampusHub. De la mise en ligne par l'enseignant à la validation par le doyen, il assure la cohérence et la disponibilité des ressources académiques.

---

## 🚀 Fonctionnalités Clés

- **Gestion de Catalogue** : CRUD complet sur les supports de cours (PDF).
- **Workflow de Validation** : Système d'états (Brouillon, Soumis, Validé, Rejeté) avec retours pédagogiques du Doyen.
- **Messagerie Asynchrone** : Notification automatique des changements d'états via RabbitMQ.
- **Filtrage Intelligent** : Accès segmenté par département, niveau (L1-M2) et matière.
- **Intégration Cloudinary** : Gestion sécurisée du stockage des fichiers.

---

## 🧬 Intégration avec les Notifications

Ce service agit comme un **producteur d'événements**. À chaque changement de statut d'un support, un message est publié sur l'exchange RabbitMQ.
- **Exchange** : `support_exchange`
- **Routing Key** : `support.notification`
- **Impact** : Déclenche une alerte temps réel chez l'utilisateur concerné via le service de notification.

---

## 🛠️ Stack Technique

- **Core :** Spring Boot 3.2.5
- **Messaging :** Spring AMQP (RabbitMQ)
- **Persistence :** Spring Data JPA
- **Base de données :** MySQL 8.0
- **Cloud Storage :** Intégration API Cloudinary

---

## 📡 API Endpoints Principaux

| Méthode | Path | Description | Accès |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/supports` | Liste des supports validés | Étudiant / Tous |
| `POST` | `/api/supports` | Dépôt d'un nouveau support | Enseignant |
| `GET` | `/api/supports/enseignant/:id` | Supports personnels de l'enseignant | Enseignant |
| `POST` | `/api/supports/:id/validate` | Validation académique du support | Doyen |
| `POST` | `/api/supports/:id/reject` | Rejet avec remarque motivée | Doyen |

---

## ⚙️ Configuration & Démarrage

### Build du package (Crucial)
Comme le `Dockerfile` utilise le fichier `.jar` généré localement, vous devez compiler le projet avant de build l'image Docker :

```bash
# Générer le JAR en sautant les tests
./mvnw clean package -DskipTests
```

### Lancement Local
```bash
./mvnw spring-boot:run
```

### Déploiement Docker
```bash
docker build -t campushub-support-service .
```

---
<p align="center">Le savoir, accessible partout et à tout moment</p>

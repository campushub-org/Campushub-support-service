# campushub-support-service - Service de Gestion des Supports de Cours

Ce service est responsable de la gestion du cycle de vie des supports de cours (documents PDF, PPT, etc.) déposés par les enseignants.

### Fonctionnalités

*   **Gestion des supports de cours** : Permet aux enseignants de créer, mettre à jour et supprimer leurs supports de cours (à l'état de brouillon).
*   **Workflow de validation** : Gère le cycle de vie d'un support (brouillon, soumis, validé, rejeté) via des endpoints dédiés.
*   **Communication Inter-Services** : 
    *   Communique avec `campushub-user-service` pour valider l'identité des enseignants.
    *   Publie des événements (ex: `support.submitted`) sur RabbitMQ pour une communication asynchrone avec d'autres services (notifications, etc.).
*   **Intégration Eureka** : S'enregistre auprès de `campushub-registry`.
*   **Configuration centralisée** : Obtient sa configuration de `campushub-config`.
*   **Persistance des données** : Utilise une base de données MySQL dédiée (`campushub-support-db`).

### Commandes Utiles

#### Construire le service (localement, sans Docker)

```bash
cd campushub-deployment/campushub-support-service
./mvnw clean install -DskipTests
```

#### Exécuter le service (localement, sans Docker)

```bash
cd campushub-deployment/campushub-support-service
java -jar target/campushub-support-service-0.0.1-SNAPSHOT.jar
```
Le service sera accessible sur le port défini dans sa configuration (par défaut 8083).

#### Construire et exécuter avec Docker Compose

Pour que Docker puisse construire l'image correctement, le fichier JAR de l'application doit être construit *au préalable* sur votre machine locale.

1.  **Construire le JAR de l'application :**
    ```bash
    cd campushub-deployment/campushub-support-service
    ./mvnw install -DskipTests
    ```
    Cette commande va compiler le code et générer le fichier `campushub-support-service-0.0.1-SNAPSHOT.jar` dans le répertoire `target/`.

2.  **Construire l'image Docker et démarrer le service :**
    ```bash
    # Depuis le répertoire campushub-deployment/
    docker compose build campushub-support-service
    docker compose up -d campushub-support-service
    ```
### Endpoints de l'API

**Note importante :** Les exemples ci-dessous supposent que le `campushub-gateway-service` est en cours d'exécution sur `http://localhost:8080` et qu'il route les requêtes avec le préfixe `/campushub-support-service` vers ce service.

---

#### 1. Créer un support de cours (Enseignant)

- **Méthode :** `POST`
- **Path :** `/api/supports`
- **Permissions :** `ROLE_TEACHER`
- **Description :** Crée un nouveau support de cours. L'ID de l'enseignant est automatiquement déduit du token d'authentification.

**Exemple `curl`:**
```bash
# Remplacez YOUR_TEACHER_JWT_TOKEN par un token valide d'enseignant
curl --location 'http://localhost:8080/campushub-support-service/api/supports' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TEACHER_JWT_TOKEN' \
--data-raw 
{
    "titre": "Introduction à la Physique Quantique",
    "description": "Chapitre 1 du cours de Physique Moderne.",
    "fichierUrl": "http://example.com/cours/physique_chap1.pdf"
}
```

---

#### 2. Soumettre un support pour validation (Enseignant)

- **Méthode :** `POST`
- **Path :** `/api/supports/{id}/submit`
- **Permissions :** Propriétaire du support & `ROLE_TEACHER`
- **Description :** Change le statut du support de `BROUILLON` à `SOUMIS` et envoie un événement pour notifier le doyen.

**Exemple `curl`:**
```bash
curl --location --request POST 'http://localhost:8080/campushub-support-service/api/supports/1/submit' \
--header 'Authorization: Bearer YOUR_TEACHER_JWT_TOKEN'
```

---

#### 3. Valider un support (Doyen / Admin)

- **Méthode :** `POST`
- **Path :** `/api/supports/{id}/validate`
- **Permissions :** `ROLE_DEAN` ou `ROLE_ADMIN`
- **Description :** Change le statut à `VALIDÉ`.

**Exemple `curl`:**
```bash
# Remplacez YOUR_DEAN_JWT_TOKEN par un token valide de doyen/admin
curl --location --request POST 'http://localhost:8080/campushub-support-service/api/supports/1/validate' \
--header 'Authorization: Bearer YOUR_DEAN_JWT_TOKEN' \
--header 'Content-Type: text/plain' \
--data-raw 'Excellent travail.'
```

---

#### 4. Rejeter un support (Doyen / Admin)

- **Méthode :** `POST`
- **Path :** `/api/supports/{id}/reject`
- **Permissions :** `ROLE_DEAN` ou `ROLE_ADMIN`
- **Description :** Change le statut à `REJETÉ`.

**Exemple `curl`:**
```bash
curl --location --request POST 'http://localhost:8080/campushub-support-service/api/supports/1/reject' \
--header 'Authorization: Bearer YOUR_DEAN_JWT_TOKEN' \
--header 'Content-Type: text/plain' \
--data-raw 'Veuillez corriger la section 2.3.'
```

---

#### 5. Lister les supports en attente (Doyen / Admin)

- **Méthode :** `GET`
- **Path :** `/api/supports/pending`
- **Permissions :** `ROLE_DEAN` ou `ROLE_ADMIN`
- **Description :** Récupère la liste de tous les supports avec le statut `SOUMIS`.

**Exemple `curl`:**
```bash
curl --location 'http://localhost:8080/campushub-support-service/api/supports/pending' \
--header 'Authorization: Bearer YOUR_DEAN_JWT_TOKEN'
```

---

#### 6. Supprimer un support (Propriétaire ou Admin)

- **Méthode :** `DELETE`
- **Path :** `/api/supports/{id}`
- **Permissions :** Propriétaire du support ou `ADMIN`
- **Description :** Supprime un support de cours (généralement s'il est encore à l'état de brouillon).

**Exemple `curl`:**
```bash
curl --location --request DELETE 'http://localhost:8080/campushub-support-service/api/supports/1' \
--header 'Authorization: Bearer YOUR_TEACHER_JWT_TOKEN'
```
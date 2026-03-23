# 📚 Documentation Détaillée - Framework MVC Mini

**Version :** 1.0  
**Date :** Mars 2026  
**Langage :** Java 17+  
**Servlet API :** Jakarta Servlet 6.0

---

## 📋 Table des Matières

1. [Introduction](#introduction)
2. [Architecture Générale](#architecture-générale)
3. [Démarrage Rapide](#démarrage-rapide)
4. [Concepts Fondamentaux](#concepts-fondamentaux)
5. [Annotations du Framework](#annotations-du-framework)
6. [Guide Complet d'Utilisation](#guide-complet-dutilisation)
7. [Gestion des Paramètres](#gestion-des-paramètres)
8. [Gestion des Fichiers](#gestion-des-fichiers)
9. [Rendu de Contenu](#rendu-de-contenu)
10. [Configuration](#configuration)
11. [Logging et Monitoring](#logging-et-monitoring)
12. [Sécurité et Authentification](#sécurité-et-authentification)
13. [Gestion des Services](#gestion-des-services)
14. [Bonnes Pratiques](#bonnes-pratiques)
15. [Troubleshooting](#troubleshooting)

---

## Introduction

### Qu'est-ce que le Framework MVC Mini ?

Le **Framework MVC Mini** est un framework web léger basé sur le pattern **Model-View-Controller** (MVC), conçu spécifiquement pour les applications Java utilisant **Jakarta Servlet 6.0**.

Ce framework simplifie le développement d'applications web en :
- Éliminant la configuration boilerplate
- Fournissant un système de routage automatique
- Supportant la gestion automatique des paramètres
- Fournissant des utilitaires pour les tâches courantes (JSON, dates, types de données)
- Offrant un système flexible de rendu de contenu

### Objectifs du Framework

✅ **Légèreté** : Minimal, sans dépendances externes lourdes  
✅ **Flexibilité** : Adapté à différents types d'applications  
✅ **Facilité d'utilisation** : API intuitive avec annotations  
✅ **Extensibilité** : Possible d'étendre les comportements principaux  

### Avantages

| Avantage | Description |
|----------|-------------|
| **Annotation-driven** | Utilisation d'annotations pour une configuration simple et claire |
| **Automatic Routing** | Découverte automatique des routes sans configuration XML |
| **Parameter Binding** | Injection automatique des paramètres avec conversion de types |
| **File Handling** | Support natif du multipart et sauvegarde atomique de fichiers |
| **Content Negotiation** | Rendu automatique (JSP, JSON, HTML, redirect) |
| **Security** | Système d'authentification et autorisation intégré |
| **Logging** | Logging structuré avec niveaux et rotation de fichiers |
| **Utilities** | Outils pour JSON, dates, conversion de types |

---

## Architecture Générale

### Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (Browser)                        │
└────────────────────────┬──────────────────────────────────────┘
                         │ HTTP Request
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              FrontControllerServlet                          │
│  • Point d'entrée unique pour toutes les requêtes           │
│  • Gestion des ressources statiques                         │
│  • Orchestration du pipeline de requête                     │
└────────────┬──────────────────────────────────────────────────┘
             │
             ├─────────────────────────────────────┐
             │                                     │
             ▼                                     ▼
    ┌─────────────────────┐          ┌──────────────────────┐
    │  Dispatch Engine    │          │  Error Handler       │
    │  • Route Matching   │          │  • 404 Response      │
    │  • Security Check   │          │  • 500 Response      │
    │  • Path Params      │          │  • Error Logging     │
    └────────┬────────────┘          └──────────────────────┘
             │
             ▼
    ┌─────────────────────────────────────────────┐
    │   ControllerMethodInvoker                   │
    │  • Dependency Injection                     │
    │  • Argument Resolution                      │
    │  • Method Invocation                        │
    └────────┬────────────────────────────────────┘
             │
             ▼
    ┌─────────────────────────────────────────────┐
    │        Controller Method                    │
    │   (User-written code)                       │
    └────────┬────────────────────────────────────┘
             │
             ▼
    ┌─────────────────────────────────────────────┐
    │    ContentRenderManager                     │
    │  • JSP Rendering                            │
    │  • JSON Serialization                       │
    │  • String Output                            │
    │  • Redirects                                │
    └─────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Response                            │
└─────────────────────────────────────────────────────────────┘
```

### Composants Principaux

#### 1. **FrontControllerServlet** (`web.servlet`)

**Responsabilités :**
- Point d'entrée unique des requêtes HTTP
- Gestion des ressources statiques (CSS, JS, images)
- Intégration avec l'API Servlet Jakarta
- Orchestration du cycle de vie de la requête

**Cycle de vie :**
```
init()
  ↓
FrameworkBootstrap initialize()
  → Load Configuration
  → Initialize Managers (Logging, Metrics, Security)
  → Scan Controllers
  → Build Route Map
  ↓
service(request, response) → pour chaque requête
  → Check if static/JSP
  → Route dispatch
  → Response rendering
```

#### 2. **FrameworkBootstrap** (`core.bootstrap`)

**Responsabilités :**
- Charge la configuration du framework
- Initialise les managers globaux
- Scanne et enregistre les contrôleurs
- Construit la carte des routes

**Managers initialisés :**
- `LogManager` : Logging centralisé
- `MetricsManager` : Métriques de performance
- `SecurityManager` : Authentification/Autorisation
- `CachedService` : Registre des services
- `ContentRenderManager` : Rendu de contenu

#### 3. **ControllerScanner** (`core.scanning`)

**Responsabilités :**
- Découverte des classes annotées `@Controller`
- Extraction des méthodes de route
- Collecte des métadonnées d'annotation

**Processus de scanning :**
```
scanWEB-INF/classes/
  └─ Find @Controller classes
      ├─ Method scanning
      │   ├─ Extract @UrlMap
      │   ├─ Extract @GetMapping/@PostMapping
      │   ├─ Extract @JsonUrl
      │   └─ Extract @Authorize
      └─ Store metadata in CachedMethodInfo
```

#### 4. **RouteRegistryBuilder** (`core.routing`)

**Responsabilités :**
- Construction de la carte des routes
- Conversion des patterns d'URL en expressions régulières
- Stockage dans le `ServletContext`

**RouteMap structure :**
```java
RouteMap {
  "/users/{id}" : {
    GET : CachedMethodInfo(UserController.getUser),
    POST : CachedMethodInfo(UserController.updateUser)
  },
  "/posts/{id}/comments/{commentId}" : {
    GET : CachedMethodInfo(PostController.getComment)
  }
}
```

#### 5. **RequestDispatcherEngine** (`core.dispatch`)

**Responsabilités :**
- Matching des URLs contre les patterns de route
- Extraction des paramètres d'URL
- Vérification des droits d'accès
- Orchestration de l'invocation

**Algorithme de dispatch :**
```
1. GET URL from request
2. FOR each route in RouteMap:
   a. Match pattern (URL against regex)
   b. If match AND HTTP method matches:
      - Check security permissions
      - Extract path parameters
      - Create CachedService context
      - Call ControllerMethodInvoker
3. If no route matches:
   - Render 404 error
```

#### 6. **ControllerMethodInvoker** (`core.invocation`)

**Responsabilités :**
- Instantiation des contrôleurs
- Injection des dépendances
- Résolution des arguments de méthode
- Invocation de la méthode avec gestion d'erreurs

**Processus d'invocation :**
```
1. Create or get Controller instance
2. Inject @Service dependencies
3. Resolve method arguments
   - @UrlParam → from URL path
   - @RequestAttribute → from query/form
   - Implicit → from request (MultipartRequestReader)
4. Validate arguments
5. Invoke target method
6. Return result to dispatcher
```

#### 7. **ContentRenderManager** (`web.response`)

**Responsabilités :**
- Conversion des résultats en réponses HTTP
- Gestion des JSP
- Sérialisation JSON
- Gestion des redirects

**Stratégies de rendu :**
```
String result
  → text/html response

ModelView result
  → forward to JSP
  → inject data in request scope

Object result + @JsonUrl
  → JSON serialization
  → application/json response

null result
  → 204 No Content
```

---

## Démarrage Rapide

### Prérequis

```
Java 17 ou supérieur
Maven 3.6 ou supérieur
Serveur Jakarta Servlet 6.0 compatible (Tomcat 10+, Jetty 11+)
```

### Installation

1. **Cloner ou récupérer le projet**

```bash
cd framework
```

2. **Compiler le framework**

```bash
mvn clean install
```

3. **Intégrer dans votre application**

Ajouter la dépendance dans votre `pom.xml` :

```xml
<dependency>
  <groupId>mg.miniframework</groupId>
  <artifactId>miniframework</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Configuration Initiale

1. **Créer le fichier de configuration**

Créer `/WEB-INF/config/application.properties` :

```properties
# Répertoire de base pour les vues JSP
jsp_base_path=/WEB-INF/views

# Répertoire pour les fichiers uploadés
upload_path=/uploads

# Répertoire des logs
miniframework.logdir=./log
```

2. **Configurer la servlet dans web.xml**

```xml
<servlet>
  <servlet-name>FrameworkServlet</servlet-name>
  <servlet-class>mg.miniframework.web.servlet.FrontControllerServlet</servlet-class>
  <load-on-startup>1</load-on-startup>
  <multipart-config>
    <max-file-size>52428800</max-file-size>
    <max-request-size>104857600</max-request-size>
  </multipart-config>
</servlet>

<servlet-mapping>
  <servlet-name>FrameworkServlet</servlet-name>
  <url-pattern>/*</url-pattern>
</servlet-mapping>
```

3. **Créer votre premier contrôleur**

```java
package com.example.controller;

import mg.miniframework.annotation.*;

@Controller(mapping = "/api/users")
public class UserController {
  
  @UrlMap("/")
  @GetMapping
  @JsonUrl
  public List<User> getAllUsers() {
    return List.of(
      new User(1, "Alice", "alice@example.com"),
      new User(2, "Bob", "bob@example.com")
    );
  }
  
  @UrlMap("/{id}")
  @GetMapping
  @JsonUrl
  public User getUser(@UrlParam(name = "id") int id) {
    return new User(id, "User " + id, "user" + id + "@example.com");
  }
}
```

4. **Démarrer le serveur**

```bash
mvn tomcat:run
```

5. **Téstez**

```bash
curl http://localhost:8080/api/users
curl http://localhost:8080/api/users/1
```

---

## Concepts Fondamentaux

### 1. Pattern MVC

Le framework suit le pattern **Model-View-Controller** :

| Composant | Rôle |
|-----------|------|
| **Model** | Données métier (objets POJO, entités) |
| **View** | Présentation (JSP, JSON, HTML) |
| **Controller** | Logique de coordination (méthodes annotées) |

### 2. Front Controller Pattern

Le `FrontControllerServlet` agit comme point d'entrée unique :
- Centralise la gestion des requêtes
- Simplifie le contrôle d'accès
- Facilite la logging/monitoring global
- Permet la gestion centralisée des erreurs

### 3. Convention over Configuration

Le framework privilégie les conventions :
- Routes basées sur les annotations (pas XML)
- Noms de paramètres explicites
- Localisation standard des vues (`/WEB-INF/views`)
- Localisation standard des logs (`./log`)

### 4. Inversion of Control (IoC)

Le framework gère :
- **Découverte** des composants
- **Instantiation** des contrôleurs
- **Injection** des dépendances
- **Résolution** des arguments de méthode

### 5. Automatic Type Conversion

La framework convertit automatiquement :
```
String "123" → int 123
String "true" → boolean true
String "2024-03-14" → LocalDate
String "Alice,Bob" → List<String>
```

---

## Annotations du Framework

### Annotations de Classe

#### @Controller
Déclare une classe comme contrôleur MVC.

```java
@Controller(mapping = "/users")
public class UserController {
  // routes will be prefixed with /users
}
```

**Attributs :**
- `mapping` : Préfixe d'URL pour toutes les routes (défaut : "")

---

### Annotations de Méthode

#### @UrlMap
Associe une méthode à une URL.

```java
@UrlMap("/")
@UrlMap("/{id}")
@UrlMap("/{id}/comments/{commentId}")
```

**Patterns supportés :**
- `/` : Racine
- `/{id}` : Paramètre simple
- `/{id}/posts` : Chemin combiné
- `/{id}/posts/{postId}` : Paramètres multiples

#### @GetMapping
Spécifie que la méthode traite les requêtes HTTP GET.

```java
@UrlMap("/{id}")
@GetMapping
public User getUser(@UrlParam(name = "id") int id) {
  // ...
}
```

#### @PostMapping
Spécifie que la méthode traite les requêtes HTTP POST.

```java
@UrlMap("/")
@PostMapping
public String createUser(User user) {
  // ...
}
```

#### @JsonUrl
Force le rendu du résultat en JSON.

```java
@UrlMap("/list")
@GetMapping
@JsonUrl
public List<User> getAllUsers() {
  return userService.findAll();
}
```

---

### Annotations de Paramètres

#### @UrlParam
Injection d'un paramètre depuis l'URL.

```java
// URL : /users/123/posts/456
@UrlMap("/{userId}/posts/{postId}")
@GetMapping
public Post getPost(
  @UrlParam(name = "userId") int userId,
  @UrlParam(name = "postId") int postId
) {
  // ...
}
```

**Attributs :**
- `name` : Nom du paramètre dans l'URL (requis)

#### @RequestAttribute
Injection d'un paramètre depuis la requête (GET/POST).

```java
// Query : ?name=Alice&page=1
@GetMapping
public String search(
  @RequestAttribute(paramName = "name", defaultValue = "") String name,
  @RequestAttribute(paramName = "page", defaultValue = "1") int page
) {
  // ...
}
```

**Attributs :**
- `paramName` : Nom du paramètre (requis)
- `defaultValue` : Valeur par défaut si absent

#### @FormParam
Injection d'un champ de formulaire.

```java
// Form data: password=secret123
@PostMapping
public String changePassword(
  @FormParam(name = "password") String password
) {
  // ...
}
```

**Attributs :**
- `name` : Nom du champ du formulaire

#### @Service
Injection d'une dépendance service.

```java
@Controller(mapping = "/users")
public class UserController {
  @Service
  UserService userService;
  
  @UrlMap("/")
  @GetMapping
  @JsonUrl
  public List<User> list() {
    return userService.findAll();
  }
}
```

#### @Authorize
Restreint l'accès à des rôles spécifiques.

```java
@UrlMap("/admin")
@GetMapping
@Authorize(roles = {"ADMIN"})
public String adminPanel() {
  // Only ADMIN role can access
}
```

**Attributs :**
- `roles` : Liste des rôles autorisés

#### @AllowAnonymous
Permet l'accès sans authentification.

```java
@UrlMap("/login")
@PostMapping
@AllowAnonymous
public String login(String username, String password) {
  // Public access
}
```

---

## Guide Complet d'Utilisation

### Cas 1 : Liste avec paramètres optionnels

```java
@Controller(mapping = "/api/products")
public class ProductController {
  
  @Service
  ProductService productService;
  
  @UrlMap("/")
  @GetMapping
  @JsonUrl
  public List<Product> searchProducts(
    @RequestAttribute(paramName = "category", defaultValue = "") String category,
    @RequestAttribute(paramName = "minPrice", defaultValue = "0") double minPrice,
    @RequestAttribute(paramName = "maxPrice", defaultValue = "999999") double maxPrice,
    @RequestAttribute(paramName = "limit", defaultValue = "20") int limit
  ) {
    return productService.search(category, minPrice, maxPrice, limit);
  }
}

// Usage : GET /api/products?category=Electronics&minPrice=100&maxPrice=1000
```

### Cas 2 : CRUD avec paramètres d'URL

```java
@Controller(mapping = "/api/articles")
public class ArticleController {
  
  @Service
  ArticleService articleService;
  
  // GET /api/articles
  @UrlMap("/")
  @GetMapping
  @JsonUrl
  public List<Article> listAll() {
    return articleService.findAll();
  }
  
  // GET /api/articles/123
  @UrlMap("/{id}")
  @GetMapping
  @JsonUrl
  public Article getById(@UrlParam(name = "id") int id) {
    return articleService.findById(id);
  }
  
  // POST /api/articles
  @UrlMap("/")
  @PostMapping
  @JsonUrl
  public Article create(Article article) {
    return articleService.save(article);
  }
  
  // POST /api/articles/123
  @UrlMap("/{id}")
  @PostMapping
  @JsonUrl
  public Article update(
    @UrlParam(name = "id") int id,
    Article article
  ) {
    article.setId(id);
    return articleService.update(article);
  }
}
```

### Cas 3 : Pages avec vues JSP

```java
@Controller(mapping = "/users")
public class UserPageController {
  
  @Service
  UserService userService;
  
  // GET /users/list → render /WEB-INF/views/users/list.jsp
  @UrlMap("/list")
  @GetMapping
  public ModelView listUsers() {
    List<User> users = userService.findAll();
    ModelView view = new ModelView("users/list");
    view.put("users", users);
    view.put("title", "Liste des utilisateurs");
    return view;
  }
  
  // GET /users/details/123 → render /WEB-INF/views/users/details.jsp
  @UrlMap("/details/{id}")
  @GetMapping
  public ModelView userDetails(@UrlParam(name = "id") int id) {
    User user = userService.findById(id);
    ModelView view = new ModelView("users/details");
    view.put("user", user);
    return view;
  }
}

// JSP : /WEB-INF/views/users/list.jsp
<%@ page contentType="text/html; charset=UTF-8" %>
<h1>${title}</h1>
<table>
  <c:forEach items="${users}" var="user">
    <tr>
      <td>${user.id}</td>
      <td>${user.name}</td>
      <td>${user.email}</td>
    </tr>
  </c:forEach>
</table>
```

### Cas 4 : Formulaires avec objets imbriqués

```java
// Modèle
public class CreateUserForm {
  private String username;
  private String email;
  private Address address; // objet imbriqué
}

public class Address {
  private String street;
  private String city;
  private String zipCode;
}

// Contrôleur
@Controller(mapping = "/admin")
public class AdminController {
  
  @UrlMap("/users/create")
  @PostMapping
  public String createUser(CreateUserForm form) {
    // form.getAddress().getCity() ne sera pas null
    userService.save(form);
    return "User created successfully!";
  }
}

// HTML Form
<form method="POST" action="/admin/users/create">
  <input name="username" placeholder="Username">
  <input name="email" placeholder="Email">
  <input name="address.street" placeholder="Street">
  <input name="address.city" placeholder="City">
  <input name="address.zipCode" placeholder="Zip Code">
  <button type="submit">Create</button>
</form>
```

### Cas 5 : Upload de fichiers

```java
@Controller(mapping = "/files")
public class FileController {
  
  @UrlMap("/upload")
  @PostMapping
  public String uploadFile(
    @RequestAttribute(paramName = "file") File uploadedFile,
    @RequestAttribute(paramName = "description") String description
  ) {
    // Sauvegarder le fichier
    uploadedFile.save("/documents");
    
    return "File uploaded: " + uploadedFile.getContextPath();
  }
  
  // Ou avec Map
  @UrlMap("/upload-multiple")
  @PostMapping
  public String uploadMultiple(
    Map<Path, File> files
  ) {
    for (Map.Entry<Path, File> entry : files.entrySet()) {
      entry.getValue().save("/documents");
    }
    return files.size() + " files uploaded";
  }
}
```

---

## Gestion des Paramètres

### Types Supportés

#### 1. Types Primitifs

```java
@UrlMap("/{id}")
@GetMapping
public void example(
  @UrlParam(name = "id") int id,               // String → int
  @UrlParam(name = "price") double price,      // String → double
  @UrlParam(name = "active") boolean active,   // String → boolean
  @UrlParam(name = "count") long count         // String → long
) {
  // ...
}
```

#### 2. Chaînes de Caractères

```java
@GetMapping
public void example(
  @RequestAttribute(paramName = "name") String name,
  @RequestAttribute(paramName = "email") String email
) {
  // ...
}
```

#### 3. Dates et Heures

```java
import java.time.LocalDate;
import java.time.LocalDateTime;

@GetMapping
public void example(
  @RequestAttribute(paramName = "birthDate") LocalDate birthDate,
  @RequestAttribute(paramName = "createdAt") LocalDateTime createdAt
) {
  // Format accepté : yyyy-MM-dd (pour LocalDate)
  // Format accepté : yyyy-MM-dd HH:mm:ss (pour LocalDateTime)
}
```

#### 4. Objets POJO

Le framework reconstruit automatiquement les objets à partir des paramètres :

```java
public class User {
  private int id;
  private String name;
  private String email;
  private int age;
  
  // getters/setters
}

@PostMapping
public String createUser(User user) {
  // Form data : name=Alice&email=alice@ex.com&age=25
  // → User { name="Alice", email="alice@ex.com", age=25 }
}
```

#### 5. Objets Imbriqués

```java
public class UserProfile {
  private String bio;
  private Address address;
  
  // getters/setters
}

public class Address {
  private String street;
  private String city;
  
  // getters/setters
}

@PostMapping
public String saveProfile(UserProfile profile) {
  // Form : bio=...&address.street=...&address.city=...
  // → UserProfile { 
  //     bio="...", 
  //     address={ street="...", city="..." }
  //   }
}
```

#### 6. Collections

```java
@PostMapping
public String bulkCreate(List<User> users) {
  // Form : users[0].name=Alice&users[0].email=...
  //        users[1].name=Bob&users[1].email=...
  // → List<User> { User(...), User(...) }
}
```

#### 7. Maps

**Map de paramètres simples :**

```java
@GetMapping
public void example(Map<String, Object> params) {
  // Tous les paramètres GET/POST sont injectés
  // params.get("name"), params.get("email"), etc.
}
```

**Map de fichiers :**

```java
@PostMapping
public String uploadMultiple(Map<Path, File> files) {
  // Tous les fichiers uploadés
  for (Map.Entry<Path, File> entry : files.entrySet()) {
    System.out.println(entry.getKey()); // nom du fichier
    System.out.println(entry.getValue().getContent()); // contenu
  }
}
```

### Conversion de Types

Le framework utilise `DataTypeUtils` pour convertir les types automatiquement :

```
String → int        : Integer.parseInt()
String → long       : Long.parseLong()
String → double     : Double.parseDouble()
String → boolean    : Boolean.parseBoolean()
String → LocalDate  : LocalDate.parse() avec pattern "yyyy-MM-dd"
String → Date       : DateUtils.parseDate()
```

### Validation

Les validations de base se font automatiquement :
- Vérification des types
- Gestion des valeurs null
- Gestion des valeurs par défaut

Pour une validation métier avancée, implémentez-la dans votre service ou utilisant un validateur externe.

---

## Gestion des Fichiers

### Classe File

Le framework fournit une classe `File` encapsulant les fichiers uploadés :

```java
public class File {
  private String contextPath;      // Chemin relatif (ex: /uploads/doc.pdf)
  private String absolutePath;     // Chemin absolu complet
  private byte[] content;          // Contenu binaire
  
  public void save(String directory) { ... }
}
```

### Upload Simple

```java
@Controller(mapping = "/upload")
public class UploadController {
  
  @UrlMap("/document")
  @PostMapping
  public String uploadDocument(
    @RequestAttribute(paramName = "file") File document
  ) {
    // Parse le fichier depuis la requête multipart
    // Save retourne le chemin
    String savedPath = document.save("/documents");
    
    return "Document saved at: " + savedPath;
  }
}

// HTML Form
<form method="POST" action="/upload/document" enctype="multipart/form-data">
  <input type="file" name="file">
  <button type="submit">Upload</button>
</form>
```

### Upload Multiple

```java
@UrlMap("/photos")
@PostMapping
public String uploadPhotos(Map<Path, File> photos) {
  for (Map.Entry<Path, File> entry : photos.entrySet()) {
    entry.getValue().save("/photos");
  }
  return photos.size() + " photos uploaded";
}

// HTML Form
<form method="POST" action="/upload/photos" enctype="multipart/form-data">
  <input type="file" name="photo1">
  <input type="file" name="photo2">
  <input type="file" name="photo3">
  <button type="submit">Upload</button>
</form>
```

### Configuration d'Upload

Configurer dans `application.properties` :

```properties
# Répertoire de base des uploads
upload_path=/uploads

# Taille maximale des fichiers (set in web.xml atau MultipartConfig)
# max-file-size: 52428800 (50MB)
```

### Sauvegarde Atomique

La classe `File` utilise une sauvegarde atomique :

```
1. Créer fichier temporaire    (.tmp)
2. Y écrire le contenu         (atomic write)
3. Renommer en fichier final   (atomic move)
```

Cela garantit qu'il n'y a jamais de fichier partiellement écrit.

### Limites

- **Taille maximale d'un fichier** : Configurable (par défaut 50MB)
- **Taille maximale de requête** : Configurable (par défaut 100MB)
- **Format** : Accepte tous les formats

---

## Rendu de Contenu

### Types de Retour

#### 1. String (texte/HTML direct)

```java
@UrlMap("/")
@GetMapping
public String helloWorld() {
  return "<h1>Hello World!</h1>";
}

// Response : 200 OK
// Content-Type : text/html;charset=UTF-8
// Body : <h1>Hello World!</h1>
```

#### 2. ModelView (JSP avec données)

```java
@UrlMap("/users")
@GetMapping
public ModelView listUsers() {
  List<User> users = userService.findAll();
  
  ModelView view = new ModelView("users/list");
  view.put("users", users);
  view.put("title", "Liste des utilisateurs");
  view.put("count", users.size());
  
  return view;
}

// Forward à /WEB-INF/views/users/list.jsp
// Données disponibles dans le scope de la requête
// ${users}, ${title}, ${count}
```

**Structure de fichiers :**

```
src/main/webapp/
└── WEB-INF/
    └── views/
        ├── users/
        │   ├── list.jsp
        │   ├── details.jsp
        │   └── form.jsp
        └── products/
            ├── list.jsp
            └── details.jsp
```

#### 3. JSON (@JsonUrl)

```java
@UrlMap("/api/users")
@GetMapping
@JsonUrl
public List<User> getUsersAsJson() {
  return userService.findAll();
}

// Response : 200 OK
// Content-Type : application/json
// Body : [{"id":1,"name":"Alice","email":"alice@ex.com"}, ...]
```

```java
@UrlMap("/api/user/{id}")
@GetMapping
@JsonUrl
public User getUserAsJson(@UrlParam(name = "id") int id) {
  return userService.findById(id);
}

// Response : 200 OK
// Content-Type : application/json
// Body : {"id":1,"name":"Alice","email":"alice@ex.com"}
```

#### 4. Null (204 No Content)

```java
@UrlMap("/process")
@PostMapping
public void processData(Data data) {
  // Effectuer le traitement
  dataService.process(data);
  
  // Aucun retour
}

// Response : 204 No Content
// Body : (empty)
```

### ContentRenderManager

Gère automatiquement le rendu selon le type retourné :

```java
ContentRenderManager manager = ...;

// Détection du type
if (result instanceof String) {
  // Rendu texte/HTML
}
else if (result instanceof ModelView) {
  // Forward JSP
}
else if (isJsonUrl) {
  // Sérialisation JSON
}
else if (result == null) {
  // 204 No Content
}
```

### Sérialisation JSON

La sérialisation JSON est sans dépendance externe `JsonUtils` :

```java
public class JsonUtils {
  public static String toJson(Object obj) {
    // Implémentation manuelle
    // Support des types : primitives, String, Date, Collections, Maps
  }
}
```

**Types supportés :**

```
✓ null                  → "null"
✓ boolean               → "true"/"false"
✓ numbers               → "42", "3.14"
✓ String                → "\"escaped string\""
✓ List/Collection       → "[...]"
✓ Map                   → "{...}"
✓ POJO                  → "{...}" (reflection)
✓ Date                  → "\"yyyy-MM-dd HH:mm:ss\""
✓ LocalDate             → "\"yyyy-MM-dd\""
```

**Échappement automatique :**

```
"                       → \"
\                       → \\
newline                 → \n
tab                     → \t
carriage return         → \r
```

---

## Configuration

### ConfigLoader

Le framework charge automatiquement la configuration au démarrage :

```java
public class ConfigLoader {
  // Charge automatiquement tous les fichiers :
  // - /WEB-INF/config/*.properties
  // - Dossier système : ${miniframework.config.dir}
  
  public static String get(String key, String defaultValue);
  public static String get(String key);
  public static int getInt(String key, int defaultValue);
  public static boolean getBoolean(String key, boolean defaultValue);
}
```

### Fichier de Configuration

**Localisation :** `/WEB-INF/config/application.properties`

```properties
# Framework Configuration

# Répertoire de base pour les vues JSP
jsp_base_path=/WEB-INF/views

# Répertoire pour les fichiers uploadés
upload_path=/uploads

# Répertoire des logs
miniframework.logdir=./log

# Autres propriétés personnalisées
app.name=My Application
app.version=1.0.0
app.debug=false
```

### Accès à la Configuration

```java
@Controller(mapping = "/config")
public class ConfigController {
  
  @UrlMap("/get/{key}")
  @GetMapping
  @JsonUrl
  public Config getConfig(@UrlParam(name = "key") String key) {
    String value = ConfigLoader.get(key, "NOT_FOUND");
    return new Config(key, value);
  }
}
```

### Variables d'Environnement

Le framework supporte les variables système :

```bash
# Définir le répertoire de logs
export miniframework.logdir=/var/log/myapp

# Lancer l'application
mvn tomcat:run
```

---

## Logging et Monitoring

### LogManager

Fournit un logging structuré avec niveaux et rotation de fichiers :

```java
public class LogManager {
  public static void error(String message);
  public static void warn(String message);
  public static void info(String message);
  public static void debug(String message);
  public static void success(String message);
  
  public static void error(String message, Throwable e);
  // ... pour chaque niveau
}
```

### Niveaux de Log

| Niveau | Usage | Exemple |
|--------|-------|---------|
| ERROR | Erreurs graves | Exception, validation échouée |
| WARN | Avertissements | Ressource manquante, config invalide |
| INFO | Informations | Démarrage, événements importants |
| DEBUG | Débogage | Entrée/sortie de fonctions, état |
| SUCCESS | Succès | Opération réussie, fichier sauvegardé |

### Utilisation

```java
import mg.miniframework.logging.LogManager;

@Controller(mapping = "/users")
public class UserController {
  
  @UrlMap("/create")
  @PostMapping
  @JsonUrl
  public User createUser(User user) {
    try {
      LogManager.info("Creating user: " + user.getEmail());
      User created = userService.save(user);
      LogManager.success("User created with ID: " + created.getId());
      return created;
    } catch (Exception e) {
      LogManager.error("Failed to create user", e);
      throw e;
    }
  }
}
```

### Format de Log

```
2026-03-14 10:23:45.123 [INFO]    FrameworkBootstrap - Bootstrap started
2026-03-14 10:23:45.234 [SUCCESS] ControllerScanner - Found 5 controllers
2026-03-14 10:23:45.345 [INFO]    RouteRegistryBuilder - Built 23 routes
2026-03-14 10:24:12.567 [DEBUG]   UserController - Resolving arguments for createUser
2026-03-14 10:24:12.678 [SUCCESS] UserService - User created with ID 42
```

### Rotation de Fichiers

Les logs sont créés quotidiennement :

```
./log/
├── log_2026-03-13.txt
├── log_2026-03-14.txt
└── log_2026-03-15.txt
```

Chaque fichier contient les logs de la journée.

### MetricsManager

Fournit des métriques de performance :

```java
public class MetricsManager {
  public static void recordRequestTime(String url, long durationMs);
  public static void recordControllerTime(String controller, String method, long durationMs);
  
  public static Map<String, Long> getMetrics();
  public static double getAverageResponseTime(String url);
}
```

---

## Sécurité et Authentification

### SecurityManager

Gère l'authentification et l'autorisation :

```java
public class SecurityManager {
  public static boolean authenticate(String username, String password);
  public static void login(String username);
  public static void logout();
  
  public static User getCurrentUser();
  public static boolean hasRole(String role);
  public static boolean hasPermission(String permission);
  
  public static boolean isAuthorized(String[] requiredRoles);
}
```

### Authentification

#### 1. Login

```java
@AllowAnonymous
@UrlMap("/login")
@PostMapping
public String login(
  @RequestAttribute(paramName = "username") String username,
  @RequestAttribute(paramName = "password") String password
) {
  if (SecurityManager.authenticate(username, password)) {
    SecurityManager.login(username);
    return "Login successful";
  }
  return "Invalid credentials";
}
```

#### 2. Logout

```java
@UrlMap("/logout")
@PostMapping
public String logout() {
  SecurityManager.logout();
  return "Logged out";
}
```

### Autorisation

#### Annotations de Sécurité

```java
@Authorize(roles = {"ADMIN"})
@UrlMap("/admin/settings")
@GetMapping
public String adminSettings() {
  // Seuls les ADMIN peuvent accéder
}

@AllowAnonymous
@UrlMap("/public/news")
@GetMapping
public String publicNews() {
  // Accès public
}
```

#### Vérification Programmatique

```java
@UrlMap("/protected")
@GetMapping
public String protectedResource() {
  if (!SecurityManager.hasRole("USER")) {
    return "Access denied";
  }
  
  User user = SecurityManager.getCurrentUser();
  return "Welcome " + user.getUsername();
}
```

### Rôles et Permissions

Le système supporte les rôles (ADMIN, USER, GUEST) et les permissions granulaires :

```java
@UrlMap("/delete/{id}")
@PostMapping
@Authorize(roles = {"ADMIN"})
public String deleteUser(@UrlParam(name = "id") int id) {
  // Seul ADMIN peut supprimer
}

// Application des permissions par défaut
// ADMIN   → Toute permission
// USER    → Lecture/création de ses propres ressources
// GUEST   → Lecture publique seulement
```

---

## Gestion des Services

### Injection de Dépendances

Les services sont injectés automatiquement via l'annotation `@Service` :

```java
@Service
UserService userService;

// Le framework crée/récupère l'instance automatiquement
```

### Création d'un Service

```java
public interface UserService {
  User findById(int id);
  List<User> findAll();
  User save(User user);
}

public class UserServiceImpl implements UserService {
  
  @Service
  UserRepository repository;
  
  @Override
  public User findById(int id) {
    return repository.findById(id);
  }
  
  @Override
  public List<User> findAll() {
    return repository.findAll();
  }
  
  @Override
  public User save(User user) {
    return repository.save(user);
  }
}
```

### ServiceRegistry

Le framework maintient un registre centralisé des services :

```java
// CachedService agit comme registry
// Le framework injecte automatiquement les services
// Les services sont cachés pour améliorer les performances

public class CachedService {
  // Stocke les instances de services
  private Map<String, Object> services = new HashMap<>();
  
  public <T> T getService(String name);
  public void registerService(String name, Object instance);
}
```

### Cycle de Vie des Services

```
FrameworkBootstrap init()
  ↓
Scan @Service annotations in controllers
  ↓
Create/register service instances
  ↓
Inject into @Service fields
  ↓
Ready for use
```

---

## Bonnes Pratiques

### 1. Organisation du Code

```
src/main/java/
├── com/example/
│   ├── controller/
│   │   ├── UserController.java
│   │   ├── ProductController.java
│   │   └── OrderController.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   └── OrderService.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Product.java
│   │   └── Order.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ProductRepository.java
│   │   └── OrderRepository.java
│   └── util/
│       ├── DateUtil.java
│       └── ValidationUtil.java
```

### 2. Nommage Cohérent

```java
// ✓ Bon
@Controller(mapping = "/api/users")
public class UserController { ... }

@UrlMap("/{id}")
@GetMapping
public User getUserById(@UrlParam(name = "id") int id) { ... }

// ✗ Mauvais
@Controller(mapping = "/u")
public class UC { ... }

@UrlMap("/{x}")
@GetMapping
public User get(@UrlParam(name = "x") int x) { ... }
```

### 3. Séparation des Responsabilités

```java
// ✓ Bon : Logique métier dans le service
@Controller(mapping = "/users")
public class UserController {
  @Service
  UserService userService;
  
  @UrlMap("/")
  @PostMapping
  public User create(User user) {
    // Appel du service
    return userService.save(user);
  }
}

public class UserService {
  public User save(User user) {
    // Validation
    if (user.getEmail().isEmpty()) {
      throw new ValidationException("Email required");
    }
    
    // Business logic
    user.setCreatedAt(LocalDateTime.now());
    return repository.save(user);
  }
}

// ✗ Mauvais : Logique métier mélangée au contrôleur
@UrlMap("/")
@PostMapping
public User create(User user) {
  if (user.getEmail().isEmpty()) {
    throw new Exception("Email required");
  }
  user.setCreatedAt(LocalDateTime.now());
  return repository.save(user);
}
```

### 4. Gestion des Erreurs

```java
// ✓ Bon
@UrlMap("/{id}")
@GetMapping
@JsonUrl
public User getUser(@UrlParam(name = "id") int id) {
  try {
    User user = userService.findById(id);
    if (user == null) {
      return notFoundError("User not found with id: " + id);
    }
    return user;
  } catch (Exception e) {
    LogManager.error("Error fetching user", e);
    return serverError("Internal server error");
  }
}

// ✗ Mauvais
@UrlMap("/{id}")
@GetMapping
@JsonUrl
public User getUser(@UrlParam(name = "id") int id) {
  return userService.findById(id); // Peut crash
}
```

### 5. Logging Approprié

```java
// ✓ Bon
@UrlMap("/")
@PostMapping
public String createUser(User user) {
  LogManager.info("Creating user: " + user.getEmail());
  try {
    User created = userService.save(user);
    LogManager.success("User created with ID: " + created.getId());
    return "User created";
  } catch (Exception e) {
    LogManager.error("Failed to create user: " + user.getEmail(), e);
    return "Error creating user";
  }
}
```

### 6. Validation des Données

```java
// ✓ Bon
@UrlMap("/")
@PostMapping
public String createUser(User user) {
  if (user.getName() == null || user.getName().trim().isEmpty()) {
    return "Name is required";
  }
  if (user.getEmail() == null || !user.getEmail().contains("@")) {
    return "Valid email is required";
  }
  
  userService.save(user);
  return "User created";
}
```

### 7. Utilisation de ModelView

```java
// ✓ Bon : Données séparées et claires
@UrlMap("/details/{id}")
@GetMapping
public ModelView userDetails(@UrlParam(name = "id") int id) {
  User user = userService.findById(id);
  ModelView view = new ModelView("users/details");
  view.put("user", user);
  view.put("title", "Détails de l'utilisateur");
  view.put("editUrl", "/users/" + id + "/edit");
  return view;
}
```

### 8. Conventions d'URLs

```
✓ Bonnes conventions
GET  /api/users              → Lister tous les utilisateurs
GET  /api/users/{id}         → Détails d'un utilisateur
POST /api/users              → Créer un utilisateur
POST /api/users/{id}         → Modifier un utilisateur
POST /api/users/{id}/delete  → Supprimer un utilisateur

✓ Collections
GET  /api/users/{userId}/posts              → Posts de l'utilisateur
GET  /api/users/{userId}/posts/{postId}     → Post spécifique

✗ Mauvaises conventions
GET  /api/getAllUsers
POST /api/createUser
POST /api/deleteUser/{id}
GET  /api/user?id=123       (préférer /{id})
```

---

## Troubleshooting

### Problème 1 : Route non trouvée (404)

**Symptôme :** "No route found for GET /api/users"

**Causes possibles :**
1. Classe non annotée `@Controller`
2. Méthode non annotée `@UrlMap`
3. Mauvais préfixe
4. Typo dans le pattern

**Solution :**

```java
// ✗ Mauvais : pas d'annotation
public class UserController {
  public List<User> getUsers() { ... }
}

// ✓ Correct
@Controller(mapping = "/api/users")
public class UserController {
  @UrlMap("/")
  @GetMapping
  public List<User> getUsers() { ... }
}
```

**Vérification :**
- Accéder à `/` pour voir la liste des routes disponibles
- Vérifier les logs au démarrage

### Problème 2 : Paramètres non injectés

**Symptôme :** Les paramètres reçus sont null

**Causes possibles :**
1. Mauvais type de paramètre
2. Mauvais nom du paramètre
3. Conversion de type échouée

**Solution :**

```java
// ✗ Mauvais : type et valeur ne correspondent pas
@UrlParam(name = "id") String id  // "id" est numérique dans l'URL

// ✓ Correct : type approprié
@UrlParam(name = "id") int id

// ✓ Ou avec valeur par défaut
@RequestAttribute(paramName = "limit", defaultValue = "20") int limit
```

### Problème 3 : Fichiers non uploadés

**Symptôme :** L'injection de `File` retourne null

**Causes possibles :**
1. Form sans `enctype="multipart/form-data"`
2. Nom du paramètre incorrect
3. Configuration multipart manquante

**Solution :**

```html
<!-- ✗ Mauvais : pas d'enctype -->
<form method="POST" action="/upload">
  <input name="file" type="file">
</form>

<!-- ✓ Correct : avec enctype -->
<form method="POST" action="/upload" enctype="multipart/form-data">
  <input name="file" type="file">
</form>
```

```xml
<!-- web.xml : configuration multipart -->
<servlet>
  <servlet-name>FrameworkServlet</servlet-name>
  <servlet-class>mg.miniframework.web.servlet.FrontControllerServlet</servlet-class>
  <multipart-config>
    <max-file-size>52428800</max-file-size>
    <max-request-size>104857600</max-request-size>
  </multipart-config>
</servlet>
```

### Problème 4 : JSP non trouvée

**Symptôme :** "View not found: users/list"

**Causes possibles :**
1. JSP dans le mauvais répertoire
2. Chemin incorrect dans ModelView
3. Configuration `jsp_base_path` incorrecte

**Solution :**

```java
// ✗ Mauvais
ModelView view = new ModelView("/users/list");  // ne pas inclure /WEB-INF/views

// ✓ Correct
ModelView view = new ModelView("users/list");
// Cherchera : /WEB-INF/views/users/list.jsp
```

**Vérifier la structure :**
```
src/main/webapp/
└── WEB-INF/
    ├── web.xml
    └── views/
        └── users/
            └── list.jsp    ← Fichier doit être ici
```

### Problème 5 : Service non injecté (null)

**Symptôme :** `NullPointerException` sur `@Service userService`

**Causes possibles :**
1. Service non implémenté
2. Classe service n'existe pas dans le classpath
3. Problème lors du scanning

**Solution :**

```java
// ✗ Mauvais : interface sans implémentation
@Service
UserService userService;  // Aucune classe n'implémente UserService

// ✓ Correct : implémentation fournie
public class UserServiceImpl implements UserService { ... }

@Service
UserService userService;  // Sera injecté automatiquement
```

### Problème 6 : JSON mal formaté

**Symptôme :** Réponse JSON invalide ou mal échappée

**Solution :**

```java
// ✓ Bon : JsonUtils gère l'échappement
@JsonUrl
public Result getResult() {
  Result r = new Result("Message with \"quotes\"");
  return r;
  // JSON : {"message":"Message with \"quotes\""}
}
```

---

## Exécution et Déploiement

### Développement Local

```bash
# Compiler
mvn clean compile

# Tests
mvn test

# Exécuter avec Tomcat
mvn tomcat:run

# Accéder à l'application
# http://localhost:8080
```

### Package Application

```bash
# Créer WAR
mvn clean package

# Fichier WAR créé : target/miniframework-1.0.0.war
```

### Deploiement sur Serveur

1. Copier le WAR sur le serveur Tomcat

```bash
cp target/miniframework-1.0.0.war /path/to/tomcat/webapps/
```

2. Redémarrer Tomcat

```bash
/path/to/tomcat/bin/catalina.sh restart
```

3. Accéder à l'application

```
http://serveur:8080/miniframework-1.0.0
```

---

## Termes de Vocabulaire

| Terme | Définition |
|-------|-----------|
| **Controller** | Classe gérant les requêtes HTTP |
| **Route** | Mapping URL → Méthode controller |
| **Pattern** | Template d'URL avec paramètres (ex: `/users/{id}`) |
| **ModelView** | Objet transportant données pour rendu JSP |
| **Binding** | Conversion requête → objet Java |
| **Service** | Classe contenant la logique métier |
| **Manager** | Service global (LogManager, SecurityManager, etc.) |
| **Multipart** | Format pour transmission fichiers + données |
| **JsonUrl** | Annotation pour rendu JSON automatique |
| **Front Controller** | Pattern d'arquitechture (servlet unique) |

---

## Ressources et Références

- **Fichier de Features** : [FEATURES.md](FEATURES.md)
- **Architecture Détaillée** : [FRAMEWORK_ARCHITECTURE_DETAILED.md](FRAMEWORK_ARCHITECTURE_DETAILED.md)
- **Fonctionnement Technique** : [HOW_IT_WORKS.md](HOW_IT_WORKS.md)
- **Plan de Refactoring** : [REFACTORING-PLAN.md](REFACTORING-PLAN.md)

---

**Document généré le 14 mars 2026**  
**Framework MVC Mini v1.0.0**  
**Jakarta Servlet 6.0 | Java 17+**

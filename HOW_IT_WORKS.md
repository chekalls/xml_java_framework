# Comment fonctionne l'application (état actuel)

Ce document décrit, de façon concise et technique, le flux d'exécution et les composants principaux du framework tel qu'il est après les récents refactorings (Phase 2 et Phase 4).

## Vue d'ensemble

- L'application expose une servlet centrale : `FrontControllerServlet` qui agit comme façade HTTP.
- L'initialisation globale est réalisée par `FrameworkBootstrap` (chargement de config, managers, registre de services, etc.).
- Les controllers sont découverts par `ControllerScanner` et leurs routes construites par `RouteRegistryBuilder` en un `RouteMap`.
- Les requêtes HTTP sont dispatchées via `RequestDispatcherEngine` vers la méthode controller correspondante.
- Les erreurs et réponses 404 sont rendues via `RequestErrorHandler` / `NotFoundResponseRenderer`.
- L'invocation d'une méthode controller est orchestrée par `ControllerMethodInvoker` (ancien `MethodManager`).

## Composants clés et responsabilités

- `FrontControllerServlet` (web.servlet)
  - Intègre avec l'API Servlet.
  - Délègue l'initialisation à `FrameworkBootstrap` lors de `init()`.
  - Lors des requêtes (`service`) gère : static files / JSP, metrics, et délègue le dispatch aux composants de routing.

- `FrameworkBootstrap` (core.bootstrap)
  - Charge la configuration, initialise `LogManager`, `MetricsManager`, `SecurityManager`, `CachedService`, `ContentRenderManager` et crée `ControllerMethodInvoker`.

- `ControllerScanner` (core.scanning)
  - Scanne le classpath / WEB-INF pour les classes annotées `@Controller` et collecte les méthodes annotées de routing.

- `RouteRegistryBuilder` (core.routing)
  - Construit un `RouteMap` (map Url -> CachedMethodInfo) et le place dans le `ServletContext`.

- `RequestDispatcherEngine` (core.dispatch)
  - Reçoit l'URL demandée et les patterns de routes.
  - Pour chaque route, vérifie le pattern et la méthode HTTP, contrôle les droits via `SecurityManager`, extrait les path params et invoque le `ControllerMethodInvoker`.
  - Rend les réponses JSON (via annotation `@JsonUrl`) ou délègue à `ContentRenderManager` pour le rendu serveur.

- `ControllerMethodInvoker` (core.invocation)
  - Crée l'instance du controller.
  - Appelle `ServiceInjector` pour injecter les dépendances annotées `@Service`.
  - Résout les arguments de la méthode (via `MethodArgumentResolver`) en utilisant `RequestObjectBinder` et `MultipartRequestReader` si nécessaire.
  - Execute la méthode cible et renvoie le résultat au dispatcher.

- `ServiceInjector` (service.injection)
  - Résout et injecte les instances de services nécessaires dans les champs annotés du controller (évite les casts dangereux vers `FrameworkService`).

- `RequestObjectBinder` (core.binding)
  - Construit des objets complexes (DTO) à partir des paramètres de la requête.

- `MultipartRequestReader` (web.multipart)
  - Lit les `Part` multipart, extrait contenu et métadonnées, et fournit des `MultipartRequestData` pour les binders.

- `MethodArgumentResolver` (core.invocation)
  - Prend la liste des paramètres d'une méthode et la transforme en valeurs concrètes pour l'appel (path params, query, body, session, fichiers).

- `ContentRenderManager` (web.response)
  - Convertit les objets renvoyés par les controllers en vues JSP / HTML / redirect / JSON selon conventions et annotations.

- `LogManager`, `MetricsManager`, `SecurityManager`, `CachedService`
  - Services transverses gérés par le bootstrap et utilisés dans l'ensemble du flux.

## Flux d'une requête (pas-à-pas)

1. `FrontControllerServlet.service()` reçoit la requête.
2. Si la requête cible un fichier statique ou une JSP, le servlet forwarde la requête.
3. Sinon :
   - Récupère (ou construit) le `RouteMap` depuis le `ServletContext`.
   - Construit les patterns de routes (`RoutePatternUtils`).
   - Appelle `RequestDispatcherEngine.dispatch(...)`.
4. `RequestDispatcherEngine` :
   - Pour chaque route candidate : compare pattern + méthode HTTP.
   - Vérifie l'accès via `SecurityManager`.
   - Extrait les `path params` (si présents).
   - Appelle `ControllerMethodInvoker.invokeCorrespondingMethod(...)` avec le `CachedMethodInfo` et le contexte (`req`, `resp`, `cachedService`).
5. `ControllerMethodInvoker` :
   - Instancie le controller (ou récupère instance s'il y a gestion particulière).
   - Injection des services via `ServiceInjector`.
   - Résolution des arguments via `MethodArgumentResolver` (utilise `RequestObjectBinder`, `MultipartRequestReader` si besoin).
   - Appel de la méthode cible et obtention du résultat.
6. `RequestDispatcherEngine` récupère le résultat :
   - Si la méthode est annotée `@JsonUrl`, renvoie JSON (`ContentRenderManager.convertToJson`).
   - Sinon délègue à `ContentRenderManager.renderContent(...)` pour JSP/redirect/view rendering.
7. Si aucune route ne correspond, `NotFoundResponseRenderer` est utilisé pour afficher la page 404.
8. En cas d'exception non gérée, `RequestErrorHandler.handleInternalError(...)` centralise le logging et la réponse 500.

## Points d'attention / prochains travaux recommandés

- Lancer une compilation complète et corriger les imports/warnings (`./mvnw -DskipTests package`).
- Renommer le package `mg.miniframework.loggin` en `mg.miniframework.logging` puis corriger tous les imports.
- Ajouter des tests unitaires pour `ServiceInjector`, `RequestObjectBinder`, `MethodArgumentResolver` et `ControllerMethodInvoker`.
- Considérer l'introduction d'une abstraction `ServiceRegistry` (interface) et transformer `CachedService` en `DefaultServiceRegistry`.

## Où modifier / étendre le comportement

- Pour changer la découverte des controllers : modifier `ControllerScanner`.
- Pour personnaliser le rendu des résultats : étendre `ContentRenderManager`.
- Pour modifier la stratégie d'injection : adapter `ServiceInjector`.
- Pour ajouter des middlewares globaux (pré/post invocation) : insérer hooks dans `ControllerMethodInvoker` ou autour de `RequestDispatcherEngine`.

---

Fichier généré automatiquement par les récents refactorings — gardez ce document à jour quand vous déplacez d'autres responsabilités.

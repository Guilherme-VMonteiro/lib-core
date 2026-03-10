# lib-core

Biblioteca compartilhada entre os backends da Lucra. Neste estágio, ela centraliza apenas os contratos base de mapeamento entre Entity e DTO.

## Estrutura de pacotes

```
br.com.lucra.core
└── mapper        # Contratos base para mapeamento Entity ↔ DTO
```

## Como usar como dependência

### 1. Configurar o repositório GitHub Packages

No `pom.xml` do seu projeto ou no `~/.m2/settings.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Guilherme-VMonteiro/lib-core</url>
    </repository>
</repositories>
```

### 2. Adicionar a dependência

```xml
<dependency>
    <groupId>br.com.lucra</groupId>
    <artifactId>lib-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Build

```bash
mvn clean verify
```

## Requisitos

- Java 21
- Maven 3.9+

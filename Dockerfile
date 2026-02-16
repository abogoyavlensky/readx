FROM clojure:temurin-25-tools-deps-1.12.4.1582-alpine AS build

WORKDIR /app

ARG BB_VERSION=1.12.213

# Install bb
RUN wget -qO- https://raw.githubusercontent.com/babashka/babashka/master/install \
    | bash -s -- --version "${BB_VERSION}" --static

# Install npm
RUN echo "http://dl-cdn.alpinelinux.org/alpine/v3.23/community" >> /etc/apk/repositories
RUN apk add --update --no-cache npm=11.6.3-r0

# npm: install Node deps
COPY package.json package-lock.json /app/
RUN npm i

# bb: install of Clojure tools and download deps
COPY bb.edn /app
RUN bb prepare

# clj: download deps
COPY deps.edn /app
RUN bb deps

# Build uberjar
COPY . /app
RUN bb build


FROM eclipse-temurin:25.0.1_8-jre-alpine
LABEL org.opencontainers.image.source=https://github.com/abogoyavlensky/readx

WORKDIR /app
COPY --from=build /app/target/standalone.jar /app/standalone.jar

EXPOSE 80
# Increase the max memory limit to your needs
CMD ["java", "-Xmx256m", "-jar", "standalone.jar"]

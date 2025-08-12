FROM mysql:latest
ENV MYSQL_DATABASE=oauth-test
ENV MYSQL_ROOT_PASSWORD=1234
EXPOSE 3306

# docker build -t oauth-test- .
# docker run -d -p 3306:3306 --name oauth-test-db oauth-test-mysql
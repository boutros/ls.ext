FROM node:4.2.1

ENV REFRESHED_AT 2015-11-09

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY package.json /usr/src/app/

RUN npm install > install.log 2>&1
COPY . /usr/src/app
VOLUME /usr/src/app/

EXPOSE 8010

CMD [ "npm", "start" ]
FROM --platform=linux/x86_64 clamav/clamav:1.4 
RUN apk add vim bash bash-doc curl htop netcat-openbsd
RUN install -d -m 755 -g "clamav" -o "clamav" "/home/clamav/socket"
WORKDIR /home/clamav
COPY eicar.txt ./
COPY clamd.conf /etc/clamav/clamd.conf 
COPY init-unprivileged /init-unprivileged 
RUN chmod 755 /init-unprivileged
USER clamav
ENTRYPOINT [ "/init-unprivileged" ]

Run the program and the result. 
When I run the program:
java Client 8.8.8.8 www.google.com.hk A
I get the output:
;;flags:qr rd ra   QUERY: 1   Answer: 5   Authority: 0   Additional: 0
;; ANSWER SECTION:
www.cnn.com         3528                CNAME         5                  www.cnn.com.vgtf.net
www.cnn.com         80                  CNAME         5                  cnn-cop.gslb
www.cnn.com         230                 A         5                  157.166.238.17
www.cnn.com         230                 A         5                  157.166.239.177
www.cnn.com         230                 A         5                  157.166.238.48
Implemetation of DNSClient. 
This DNSClient is to construct a DNS message request from input arguments(DNServerIP, DomainName and QueryType) and send it to server
through UDP, then get the response and get IP address of the requested hostname. 
I first convert hexidecimal string by ToHexString() to compare the result with Wireshark packet. Then I convert the hexidecimal string to byte array andcreate sendPacket through DatagramPacket, then send it to socket. 
I use wireshark to capture the request and resonse when I run the DNSClient.Then I get the receive data using receivePacket.getData(). Then I using bit operation to get information such as: numOfQuestion, numOfAnswer, Authority RR, Additional RR, Type, Class, and RDATA(which is the IPAddress). 
Finally I output the information I get in format as the output of "dig command".

Reference: 
1. http://www.zytrax.com/books/dns/ch15/#answer  for the Header,Question and Answer format. 
2. http://www.asciitable.com/   
3. http://en.wikipedia.org/wiki/Dig_%28command%29 check the output format of Answer and Question Format. 


 

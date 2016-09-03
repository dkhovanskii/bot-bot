Description
It translates non english text to english by default.  You can also get the bot to translate a piece of text to one of the supported languages.
Usage:

/lang

Shows a list of supported languages

/{lang} {text}

Translates {text} into {lang}

To run:

To get the client id and client secret follow the instructions here:
https://www.microsoft.com/en-us/translator/getstarted.aspx

Required System Properties:

        -Dkeystore.password=(Pass)
        -Dtruststore.password=(Pass)
        -Dsessionauth.url=https://(pod-host).symphony.com:8444/sessionauth
        //Note: you may have local HSM vs pod
        -Dkeyauth.url=https://(pod-host).symphony.com:8444/keyauth
        -Dsymphony.agent.pod.url=https://(symagent-host).symphony.com:8444/pod
        -Dsymphony.agent.agent.url=https://(symagent-host).symphony.com:8444/agent
        -Dcerts.dir=/dev/certs/
        -Dtruststore.file=/dev/certs/server.truststore
        -Dbot.user=(user name)
        -Dtranslate.clientid=(clientId)
        -Dtranslate.clientsecret=(clientSecret)

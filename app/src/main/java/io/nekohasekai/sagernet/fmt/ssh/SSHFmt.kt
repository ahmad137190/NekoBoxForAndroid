package io.nekohasekai.sagernet.fmt.ssh

import io.nekohasekai.sagernet.Key
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import io.nekohasekai.sagernet.ktx.ChCrypto
fun buildSingBoxOutboundSSHBean(bean: SSHBean,   hash: Boolean = false): SingBoxOptions.Outbound_SSHOptions {
    if (hash)
        return SingBoxOptions.Outbound_SSHOptions().apply {
            type = "ssh"
            // server = bean.serverAddress
            server = ChCrypto.aesEncrypt(bean.serverAddress, Key.KEY_HASH)
            server_port = bean.serverPort
            //  server_port = bean.serverPort
            //  user = bean.username
            user = ChCrypto.aesEncrypt(bean.username, Key.KEY_HASH)
            if (bean.publicKey.isNotBlank()) {
                host_key = bean.publicKey.split("\n")
            }
            when (bean.authType) {
                SSHBean.AUTH_TYPE_PRIVATE_KEY -> {
                    private_key = bean.privateKey
                    private_key_passphrase = bean.privateKeyPassphrase
                }
                else -> {
                    // password = bean.password
                    password = ChCrypto.aesEncrypt(bean.password, Key.KEY_HASH)
                }
            }
        }
    else
    return SingBoxOptions.Outbound_SSHOptions().apply {
        type = "ssh"
        server = bean.serverAddress
        server_port = bean.serverPort
        user = bean.username
        if (bean.publicKey.isNotBlank()) {
            host_key = bean.publicKey.listByLineOrComma()
        }
        when (bean.authType) {
            SSHBean.AUTH_TYPE_PRIVATE_KEY -> {
                private_key = bean.privateKey
                private_key_passphrase = bean.privateKeyPassphrase
            }
            else -> {
                password = bean.password
            }
        }
    }
}

package soushin.sandbox.kt.client.grpc

import com.squareup.okhttp.ConnectionSpec
import com.squareup.okhttp.TlsVersion
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.okhttp.NegotiationType
import io.grpc.okhttp.OkHttpChannelBuilder
import io.kotlintest.matchers.shouldNotBe
import org.junit.Ignore
import org.junit.Test
import soushin.sandbox.protobuf.EchoGrpc
import soushin.sandbox.protobuf.EchoInbound
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory

class GrpcClientTest {

    private val DOMAIN = "your-host"
    private val PORT = 50051
    private val CERT_PATH = "cert/your-cert"

    @Ignore
    @Test
    fun request_withNettyChannelBuilder() {
        val stub = EchoGrpc.newBlockingStub(getNettyChannel())
        val inbound = EchoInbound.newBuilder()
                .setMessage("Hello")
                .build()

        val outbound = stub.getEcho(inbound)

        outbound.message shouldNotBe null
    }

    private fun getNettyChannel(): ManagedChannel {
        return NettyChannelBuilder
                .forAddress(DOMAIN, PORT)
                .sslContext(
                        GrpcSslContexts.forClient()
                                .trustManager(File(CERT_PATH)).build())
                .intercept(AuthInterceptor())
                .build()
    }

    @Ignore
    @Test
    fun request_withOkHttpChannelBuilder() {
        val stub = EchoGrpc.newBlockingStub(getNettyChannel())
        val inbound = EchoInbound.newBuilder()
                .setMessage("Hello")
                .build()

        val outbound = stub.getEcho(inbound)

        outbound.message shouldNotBe null
    }

    private fun getOkHttpChannel(): ManagedChannel {

        val ins = File(CERT_PATH).inputStream()
        val sslSocket = getSslSocketFactory(ins)

        return OkHttpChannelBuilder
                .forAddress(DOMAIN, PORT)
                .sslSocketFactory(sslSocket)
                .negotiationType(NegotiationType.TLS)
                .connectionSpec(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build())
                .intercept(AuthInterceptor())
                .build()
    }

    private fun getSslSocketFactory(testCa: InputStream?): SSLSocketFactory {
        if (testCa == null) {
            return SSLSocketFactory.getDefault() as SSLSocketFactory
        }

        val context = SSLContext.getInstance("TLS")
        context.init(null, getTrustManagers(testCa), null)
        return context.getSocketFactory()
    }

    private fun getTrustManagers(testCa: InputStream): Array<TrustManager> {
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null)
        val cf = CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(testCa) as X509Certificate
        val principal = cert.getSubjectX500Principal()
        ks.setCertificateEntry(principal.getName("RFC2253"), cert)
        // Set up trust manager factory to use our key store.
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(ks)
        return trustManagerFactory.getTrustManagers()
    }

}

class AuthInterceptor : ClientInterceptor {

    private val CUSTOM_HEADER_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    private val AUTH_TOKEN = "your-auth-token"

    override fun <ReqT : Any?, RespT : Any?> interceptCall(method: MethodDescriptor<ReqT, RespT>?, callOptions: CallOptions?, next: Channel?): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next?.newCall(method, callOptions)) {

            override fun start(responseListener: ClientCall.Listener<RespT>, headers: Metadata) {
                /* put custom header */
                headers.put(CUSTOM_HEADER_KEY, AUTH_TOKEN)
                super.start(object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    override fun onHeaders(headers: Metadata) {
                        super.onHeaders(headers)
                    }
                }, headers)
            }
        }
    }
}

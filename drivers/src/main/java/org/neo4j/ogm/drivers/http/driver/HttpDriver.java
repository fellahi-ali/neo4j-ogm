package org.neo4j.ogm.drivers.http.driver;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.neo4j.ogm.authentication.Credentials;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.drivers.AbstractConfigurableDriver;
import org.neo4j.ogm.drivers.http.request.HttpAuthorization;
import org.neo4j.ogm.drivers.http.request.HttpRequest;
import org.neo4j.ogm.drivers.http.transaction.HttpTransaction;
import org.neo4j.ogm.exception.ResultErrorsException;
import org.neo4j.ogm.exception.ResultProcessingException;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vince
 */

public final class HttpDriver extends AbstractConfigurableDriver {

    private final CloseableHttpClient transport = HttpClients.createDefault();
    private final Logger logger = LoggerFactory.getLogger(HttpDriver.class);

    public HttpDriver() {
        configure(new Configuration("http.driver.properties"));
    }

    @Override
    public void close() {
        try {
            transport.close();
        } catch (Exception e) {
            logger.warn("Unexpected Exception when closing http client transport: ", e);
        }
    }

    @Override
    public Request requestHandler() {
        String url = requestUrl();
        return new HttpRequest(transport, url, (Credentials) driverConfig.getConfig("credentials"));
    }

    @Override
    public Transaction newTransaction() {

        String url = newTransactionUrl();
        return new HttpTransaction(transactionManager, this, url);
    }

    // TODO: move this !

    public CloseableHttpResponse executeHttpRequest(HttpRequestBase request) {

        try {

            request.setHeader(new BasicHeader("Accept", "application/json;charset=UTF-8"));


            HttpAuthorization.authorize(request, (Credentials) driverConfig.getConfig("credentials"));

            CloseableHttpResponse response = transport.execute(request);
            StatusLine statusLine = response.getStatusLine();

            logger.debug("Status code: {}", statusLine.getStatusCode());
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(
                        statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            // close the content stream/release the connection
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String responseText = EntityUtils.toString(responseEntity);
                logger.debug(responseText);
                EntityUtils.consume(responseEntity);
                if (responseText.contains("\"errors\":[{") || responseText.contains("\"errors\": [{")) {
                    throw new ResultErrorsException(responseText);
                }
            }
            return response;
        }

        catch (Exception e) {
            throw new ResultProcessingException("Failed to execute request: ", e);
        }

        finally {
            request.releaseConnection();
        }
    }

    private String newTransactionUrl() {
        String url = transactionEndpoint((String) driverConfig.getConfig("server"));
        logger.debug("POST {}", url);
        HttpPost request = new HttpPost(url);
        request.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
        org.apache.http.HttpResponse response = executeHttpRequest(request);
        Header location = response.getHeaders("Location")[0];
        return location.getValue();
    }

    private String autoCommitUrl() {
        return transactionEndpoint((String) driverConfig.getConfig("server")).concat("/commit");
    }

    private String transactionEndpoint(String server) {
        if (server == null) {
            return server;
        }
        String url = server;

        if (!server.endsWith("/")) {
            url += "/";
        }
        return url + "db/data/transaction";
    }

    private String requestUrl() {
        if (transactionManager != null) {
            Transaction tx = transactionManager.getCurrentTransaction();
            if (tx != null) {
                logger.debug("request url {}", ((HttpTransaction) tx).url());
                return ((HttpTransaction) tx).url();
            } else {
                logger.debug("no current transaction");
            }
        } else {
            logger.debug("no transaction manager");
        }
        logger.debug("request url {}", autoCommitUrl());
        return autoCommitUrl();
    }
}
package er.woadaptor;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDelayedCallbackCenter;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

/**
* @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
* @author Andy Taylor (andy.taylor@jboss.org)
* @author <a href="http://gleamynode.net/">Trustin Lee</a>
* 
* {@link} http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/http/snoop/HttpRequestHandler.html
*
* @author ravim ERWOAdaptor version
*/
public class WONettyAdaptorRequestHandler extends SimpleChannelUpstreamHandler {

	private HttpRequest request;
	/** Buffer that stores the response content */
    private static WOResponse _lastDitchErrorResponse;
    private WOResponse woresponse = _lastDitchErrorResponse;
	
	static {
        _lastDitchErrorResponse = new WOResponse();
        _lastDitchErrorResponse.setStatus(500);
        _lastDitchErrorResponse.setContent("An error occured");
        _lastDitchErrorResponse.setHeaders(NSDictionary.EmptyDictionary);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        request = (HttpRequest) e.getMessage();
        NSMutableDictionary<String, NSMutableArray<String>> headers = new NSMutableDictionary<String, NSMutableArray<String>>();
        for (Map.Entry<String, String> header: request.getHeaders()) {
        	headers.setObjectForKey(new NSMutableArray<String>(header.getValue()), header.getKey());
        }
        WORequest worequest = WOApplication.application().createRequest(
        		request.getMethod().getName(), 
        		request.getUri(), 
        		request.getProtocolVersion().getText(), 
        		headers,
        		new NSData(request.getContent().array()), 
        		null);
        
        try {
            boolean process = request != null;
            process &= !(!WOApplication.application().isDirectConnectEnabled() && !worequest.isUsingWebServer());
            process &= !"womp".equals(worequest.requestHandlerKey());

            if (process) {
                woresponse = WOApplication.application().dispatchRequest(worequest);
                NSDelayedCallbackCenter.defaultCenter().eventEnded();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        writeResponse(e);
	}

	private void writeResponse(MessageEvent e) {
		// Decide whether to close the connection or not.
		boolean keepAlive = isKeepAlive(request);

		// Build the response object.
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		response.setContent(ChannelBuffers.copiedBuffer(woresponse.contentString(), woresponse.contentEncoding()));
		String contentType = woresponse.headerForKey(CONTENT_TYPE);
		if (contentType != null) response.setHeader(CONTENT_TYPE, contentType);

		if (keepAlive) {
			// Add 'Content-Length' header only for a keep-alive connection.
			response.setHeader(CONTENT_LENGTH, woresponse.content().bytes().length);
		}

		// Encode the cookie.
		String cookieString = request.getHeader(COOKIE);
		if (cookieString != null) {
			CookieDecoder cookieDecoder = new CookieDecoder();
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			if(!cookies.isEmpty()) {
				// Reset the cookies if necessary.
				CookieEncoder cookieEncoder = new CookieEncoder(true);
				for (Cookie cookie : cookies) {
					cookieEncoder.addCookie(cookie);
				}
				response.addHeader(SET_COOKIE, cookieEncoder.encode());
			}
		}

		// Write the response.
		ChannelFuture future = e.getChannel().write(response);

		// Close the non-keep-alive connection after the write operation is done.
		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}
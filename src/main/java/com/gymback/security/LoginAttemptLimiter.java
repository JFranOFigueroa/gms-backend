package com.gymback.security;

import com.gymback.shared.exceptions.RateLimitException;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;

/** Bounded, single-JVM sliding-window limiter. No passwords or email addresses are retained. */
@Component
public class LoginAttemptLimiter {
  private static final long WINDOW_MS=Duration.ofMinutes(15).toMillis();
  private static final int ACCOUNT_FAILURES=5;
  private static final int IP_FAILURES=60;
  private static final int MAX_KEYS=20_000;
  private static final long FIRST_IP_PAUSE_MS=30_000L;
  private final Map<String,Bucket> buckets=new LinkedHashMap<>(128,0.75f,true);
  private final LongAdder blocked=new LongAdder();
  private final LongAdder failed=new LongAdder();

  public synchronized void check(String email,String ip) {
    long now=System.currentTimeMillis();
    String account="account:"+hash(normalize(email));
    String client="ip:"+hash(ip);
    int wait=Math.max(retrySeconds(account,ACCOUNT_FAILURES,true,now),retrySeconds(client,IP_FAILURES,false,now));
    if(wait>0){blocked.increment();throw new RateLimitException(wait);}
    // Reserve both slots before BCrypt: parallel requests cannot bypass the limit.
    buckets.computeIfAbsent(account,ignored->new Bucket()).inFlight++;
    buckets.computeIfAbsent(client,ignored->new Bucket()).inFlight++;
  }

  public synchronized void failed(String email,String ip) {
    long now=System.currentTimeMillis();
    failed.increment();
    String account="account:"+hash(normalize(email)),client="ip:"+hash(ip);
    release(account);release(client);
    register(account,ACCOUNT_FAILURES,true,now);
    register(client,IP_FAILURES,false,now);
    shrink(now);
  }

  public synchronized void succeeded(String email,String ip) {
    String account="account:"+hash(normalize(email));
    String client="ip:"+hash(ip);
    release(account);release(client);
    Bucket b=buckets.get(account);
    if(b!=null){b.failures.clear();b.blockedUntil=0;if(b.inFlight==0)buckets.remove(account);}
    removeEmpty(client);
    shrink(System.currentTimeMillis());
  }

  /** Release reservations on unexpected internal errors without recording bad credentials. */
  public synchronized void aborted(String email,String ip) {
    String account="account:"+hash(normalize(email)),client="ip:"+hash(ip);
    release(account);release(client);
    removeEmpty(account);removeEmpty(client);
    shrink(System.currentTimeMillis());
  }

  /** Numeric metrics, available to observability integration without leaking identities. */
  public long blockedCount(){return blocked.sum();}
  public long failedCount(){return failed.sum();}

  private int retrySeconds(String key,int limit,boolean strictWindow,long now) {
    Bucket bucket=buckets.get(key);
    if(bucket==null)return 0;
    expire(bucket,now);
    if(bucket.failures.isEmpty() && now>=bucket.blockedUntil && bucket.inFlight==0){buckets.remove(key);return 0;}
    if(strictWindow && bucket.failures.size()+bucket.inFlight>=limit){
      long remaining=!bucket.failures.isEmpty()?bucket.failures.peekFirst()+WINDOW_MS-now:1000L;
      return Math.max(1,(int)Math.ceil(remaining/1000.0));
    }
    if(now<bucket.blockedUntil)return Math.max(1,(int)Math.ceil((bucket.blockedUntil-now)/1000.0));
    // At the IP threshold allow a new attempt after the pause, then increase the next pause.
    if(!strictWindow && bucket.failures.size()<limit && bucket.failures.size()+bucket.inFlight>=limit)return 1;
    return 0;
  }

  private void register(String key,int limit,boolean strictWindow,long now) {
    Bucket b=buckets.computeIfAbsent(key,ignored->new Bucket());
    expire(b,now);b.failures.addLast(now);
    if(!strictWindow && b.failures.size()>=limit){
      int excess=b.failures.size()-limit;
      long pause=Math.min(WINDOW_MS,FIRST_IP_PAUSE_MS*(1L<<Math.min(excess,5)));
      b.blockedUntil=Math.max(b.blockedUntil,now+pause);
    }
  }

  private void release(String key){
    Bucket b=buckets.get(key);
    if(b!=null && b.inFlight>0)b.inFlight--;
  }

  private void removeEmpty(String key){
    Bucket b=buckets.get(key);
    if(b!=null && b.failures.isEmpty() && b.inFlight==0 && b.blockedUntil<=System.currentTimeMillis())
      buckets.remove(key);
  }

  private void expire(Bucket bucket,long now){
    while(!bucket.failures.isEmpty() && bucket.failures.peekFirst()<=now-WINDOW_MS)bucket.failures.removeFirst();
  }

  private void shrink(long now){
    if(buckets.size()<=MAX_KEYS)return;
    Iterator<Map.Entry<String,Bucket>> it=buckets.entrySet().iterator();
    while(it.hasNext()){
      Bucket b=it.next().getValue();expire(b,now);
      if(b.failures.isEmpty() && b.blockedUntil<=now && b.inFlight==0)it.remove();
    }
    // Bounded under adversarial random-account floods; evictions are an explicit single-node limitation.
    it=buckets.entrySet().iterator();
    while(buckets.size()>MAX_KEYS && it.hasNext()){
      if(it.next().getValue().inFlight==0)it.remove();
    }
  }

  private String normalize(String email){return email==null?"":email.strip().toLowerCase(Locale.ROOT);}
  private String hash(String value){
    try {
      byte[] bytes=MessageDigest.getInstance("SHA-256").digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  private static final class Bucket {
    private final ArrayDeque<Long> failures=new ArrayDeque<>();
    private long blockedUntil;
    private int inFlight;
  }
}

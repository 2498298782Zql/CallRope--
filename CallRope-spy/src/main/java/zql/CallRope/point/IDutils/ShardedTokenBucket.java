package zql.CallRope.point.IDutils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class ShardedTokenBucket {
    private long tokensPerSecond;
    private long bucketCapacity;
    private final int numShards;
    private final Shard[] shardsBucket;
    private long tokensPerSecondForShard;
    private long bucketCapacityForShard;

    private static class Shard {
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTime;
        public Shard(long initalTokens) {
            this.tokens = new AtomicLong(initalTokens);
            this.lastRefillTime = new AtomicLong(System.nanoTime());
        }
    }

    public ShardedTokenBucket(long tokensPerSecond, long bucketCapacity, int numShards) {
        this.tokensPerSecond = tokensPerSecond;
        this.bucketCapacity = bucketCapacity;
        this.numShards = numShards;
        this.shardsBucket = new Shard[numShards];
        this.tokensPerSecondForShard = tokensPerSecond / numShards;
        this.bucketCapacityForShard = bucketCapacity / numShards;
        for (int i = 0; i < numShards; i++) {
            shardsBucket[i] = new Shard(tokensPerSecond / numShards);
        }
    }

    public boolean tryConsume(long tokenNums){
        int shardIndex = ThreadLocalRandom.current().nextInt(numShards);
        Shard shard = shardsBucket[shardIndex];
        refillTokens(shard);
        long curremtTokens = shard.tokens.get();
        if(curremtTokens >= tokenNums){
            return shard.tokens.compareAndSet(curremtTokens, curremtTokens - tokenNums);
        }
        return false;
    }


    public void refillTokens(Shard shard){
        long now = System.nanoTime();
        long lastTime = shard.lastRefillTime.get();
        long elapsedTime = now - lastTime;
        if(elapsedTime > 0){
            long addTokenCount = (elapsedTime * tokensPerSecondForShard) / 1_000_000_000L;
            long currentTokenCount;
            long newTokenCount;
            do{
                currentTokenCount = shard.tokens.get();
                newTokenCount = Math.min(bucketCapacityForShard, currentTokenCount + addTokenCount);
            }while (!shard.tokens.compareAndSet(currentTokenCount, newTokenCount));
        }
    }

    public void setTokensPerSecond(long tokensPerSecond) {
        this.tokensPerSecond = tokensPerSecond;
    }

    public void setBucketCapacity(long bucketCapacity) {
        this.bucketCapacity = bucketCapacity;
    }

    public void setTokensPerSecondForShard(long tokensPerSecondForShard) {
        this.tokensPerSecondForShard = tokensPerSecondForShard;
    }

    public void setBucketCapacityForShard(long bucketCapacityForShard) {
        this.bucketCapacityForShard = bucketCapacityForShard;
    }
}

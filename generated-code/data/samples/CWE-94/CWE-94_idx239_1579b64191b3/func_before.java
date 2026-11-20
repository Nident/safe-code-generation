@Override
        protected InternalAggregation adapt(InternalAggregation delegateResult) {
            InternalDateRange range = (InternalDateRange) delegateResult;
            List<InternalDateHistogram.Bucket> buckets = new ArrayList<>(range.getBuckets().size());
            for (InternalDateRange.Bucket rangeBucket : range.getBuckets()) {
                if (rangeBucket.getDocCount() > 0) {
                    buckets.add(
                        new InternalDateHistogram.Bucket(
                            rangeBucket.getFrom().toInstant().toEpochMilli(),
                            rangeBucket.getDocCount(),
                            keyed,
                            format,
                            rangeBucket.getAggregations()
                        )
                    );
                }
            }
            CollectionUtil.introSort(buckets, BucketOrder.key(true).comparator());

            InternalDateHistogram.EmptyBucketInfo emptyBucketInfo = minDocCount == 0
                ? new InternalDateHistogram.EmptyBucketInfo(rounding.withoutOffset(), buildEmptySubAggregations(), extendedBounds)
                : null;
            return new InternalDateHistogram(
                range.getName(),
                buckets,
                order,
                minDocCount,
                rounding.offset(),
                emptyBucketInfo,
                format,
                keyed,
                range.getMetadata()
            );
        }
public KeyEquals and(KeyEquals joins) {
        MMap<KeyEqual, Where> result = MapFact.mMap(AbstractWhere.<KeyEqual>addOr());
        // берем все пары joins'ов
        for(int i1=0,size1=size();i1<size1;i1++)
            for(int i2=0,size2=joins.size();i2<size2;i2++) {
                KeyEqual eq1 = getKey(i1);
                KeyEqual eq2 = joins.getKey(i2);

                Where where1 = getValue(i1);
                Where where2 = joins.getValue(i2);

                // сначала определяем общие ключи
                Result<ImMap<ParamExpr, BaseExpr>> diffEq1 = new Result<>();
                Result<ImMap<ParamExpr, BaseExpr>> diffEq2 = new Result<>();
                ImMap<ParamExpr, BaseExpr> sameEq1 = eq1.keyExprs.splitKeys(eq2.keyExprs.keys(), diffEq1);
                ImMap<ParamExpr, BaseExpr> sameEq2 = eq2.keyExprs.splitKeys(eq1.keyExprs.keys(), diffEq2);

                // транслируем правые левыми, погнали топологическую сортировку отсеивая Expr'ы
                java.util.Map<ParamExpr, Expr> mTransEq1 = MapFact.mAddRemoveMap(); // remove есть
                MapFact.addJavaAll(mTransEq1, new PartialKeyExprTranslator(diffEq2.result).translate(diffEq1.result));

                MExclMap<ParamExpr, Expr> mCleanEq1 = MapFact.mExclMap();// складываются "очищенные" equals'ы

                while(!mTransEq1.isEmpty()) {
                    boolean found = false;
                    Iterator<java.util.Map.Entry<ParamExpr,Expr>> it = mTransEq1.entrySet().iterator();
                    while(it.hasNext()) {
                        java.util.Map.Entry<ParamExpr,Expr> keyEq = it.next();
                        ImSet<ParamExpr> enumKeys = keyEq.getValue().getOuterKeys();
                        if(MapFact.disjointJava(enumKeys, mTransEq1.keySet())) {// если не зависит от остальных
                            mCleanEq1.exclAdd(keyEq.getKey(), keyEq.getValue().translateExpr(new PartialKeyExprTranslator(mCleanEq1.immutableCopy()))); // транслэйтим clean'ами
                            it.remove();
                            found = true; 
                            break;
                        }
                    }

                    if(!found) { // значит остались циклы, берем любую и перекидываем в where
                        it = mTransEq1.entrySet().iterator();
                        java.util.Map.Entry<ParamExpr, Expr> cycle = it.next();
                        it.remove();
                        where1 = where1.and(cycle.getKey().compare(cycle.getValue(), Compare.EQUALS));
                    }
                }
                ImMap<ParamExpr, Expr> cleanEq1 = mCleanEq1.immutable();

                // второй просто транслируем первым
                PartialKeyExprTranslator cleanTranslator1 = new PartialKeyExprTranslator(cleanEq1);
                ImMap<ParamExpr, Expr> cleanEq2 = cleanTranslator1.translate(diffEq2.result);
                PartialKeyExprTranslator cleanTranslator2 = new PartialKeyExprTranslator(cleanEq2);
                Where andWhere = where2.translateExpr(cleanTranslator1).and(where1.translateExpr(cleanTranslator2));

                // сливаем same'ы, их также надо translateOuter'ить так как могут быть несвободными от противоположных ключей
                ImMap<ParamExpr, Expr> transSameEq1 = cleanTranslator2.translate(sameEq1);
                ImMap<ParamExpr, Expr> transSameEq2 = cleanTranslator1.translate(sameEq2);

                assert BaseUtils.hashEquals(sameEq1.keys(), sameEq2.keys());
                Where extraWhere = Where.TRUE();
                for(int i=0,size=transSameEq2.size();i<size;i++) {
                    ParamExpr key = transSameEq2.getKey(i);
                    Expr value2 = transSameEq2.getValue(i);
                    Expr value1 = transSameEq1.get(key);
                    if(!BaseUtils.hashEquals(value1, value2)) // закидываем compare
                        extraWhere = extraWhere.and(value1.compare(value2, Compare.EQUALS));
                }
                ImMap<ParamExpr, Expr> mergeSame = transSameEq1.merge(transSameEq2, KeyEqual.keepValue()); // предпочитаем статичные значение

                ImMap<ParamExpr, Expr> mergeKeys = mergeSame.addExcl(cleanEq1.addExcl(cleanEq2));

                Result<ImMap<ParamExpr, Expr>> notBaseExprs = new Result<>();
                ImMap<ParamExpr, BaseExpr> andEq = BaseUtils.immutableCast(mergeKeys.splitKeys((key, value) -> value instanceof BaseExpr, notBaseExprs));
                for(int i=0,size=notBaseExprs.result.size();i<size;i++)
                    extraWhere = extraWhere.and(notBaseExprs.result.getKey(i).compare(notBaseExprs.result.getValue(i), Compare.EQUALS));

                andWhere = andWhere.and(extraWhere);
                if (cleanEq1.isEmpty() && cleanEq2.isEmpty() && extraWhere.isTrue()) { // чтобы не уходило в бесконечный цикл
                    if(!andWhere.isFalse())
                        result.add(new KeyEqual(andEq), andWhere);
                } else {
                    KeyEquals recEquals = andWhere.getKeyEquals();
                    for(int i=0,recSize=recEquals.size();i<recSize;i++) {
                        KeyEqual recEqual = recEquals.getKey(i);
                        result.add(new KeyEqual((ImMap) (Object) recEqual.getTranslator().translate(andEq).addExcl(recEqual.keyExprs)), recEquals.getValue(i));
                    }
                }
            }
        return new KeyEquals(result.immutable(), false); // потому что вызывается из calculateGroupKeyEquals, а там уже проверили что все не isSimple
    }
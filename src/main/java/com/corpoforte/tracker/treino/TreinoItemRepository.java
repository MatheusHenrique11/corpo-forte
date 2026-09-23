package com.corpoforte.tracker.treino;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreinoItemRepository extends JpaRepository<TreinoItem, Long> {

    List<TreinoItem> findByTreinoDoDiaId(Long treinoDoDiaId);

    List<TreinoItem> findByTreinoDoDiaIdInAndConcluidoTrue(List<Long> treinoDoDiaIds);
}

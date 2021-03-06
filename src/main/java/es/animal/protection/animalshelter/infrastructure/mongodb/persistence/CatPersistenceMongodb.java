package es.animal.protection.animalshelter.infrastructure.mongodb.persistence;

import es.animal.protection.animalshelter.domain.exceptions.ConflictException;
import es.animal.protection.animalshelter.domain.exceptions.NotFoundException;
import es.animal.protection.animalshelter.domain.model.Cat;
import es.animal.protection.animalshelter.domain.persistence.CatPersistence;
import es.animal.protection.animalshelter.infrastructure.mongodb.daos.CatReactive;
import es.animal.protection.animalshelter.infrastructure.mongodb.entities.CatEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class CatPersistenceMongodb implements CatPersistence {

    private CatReactive catReactive;

    @Autowired
    public CatPersistenceMongodb(CatReactive catReactive) {
        this.catReactive = catReactive;
    }

    @Override
    public Mono<Cat> create(Cat cat) {
        return this.assertCatNotExist(cat.getChip())
                .then(this.catReactive.save(new CatEntity(cat))
                        .flatMap(catEntity -> Mono.just(catEntity.toCat())));
    }

    @Override
    public Mono<Cat> read(Integer chip) {
        return this.assertCatExist(chip)
                .flatMap(catEntity -> Mono.just(catEntity.toCat()));
    }

    @Override
    public Mono<Cat> update(Mono<CatEntity> catEntityUpdate) {
        return catEntityUpdate.flatMap(catEntity -> {
            return this.catReactive.save(catEntity)
                    .flatMap(catEnt -> Mono.just(catEnt.toCat()));
        });
    }

    @Override
    public Mono<Void> delete(Integer chip) {
        return this.assertCatExist(chip)
                .flatMap(catEntity -> this.catReactive.delete(catEntity));
    }

    @Override
    public Flux<Cat> findBySociableIsTrueAndDepartureDateIsNull(boolean onlyAdoptable) {
        if (onlyAdoptable) {
            return this.catReactive.findBySociableIsTrueAndDepartureDateIsNull()
                    .map(catEntity -> catEntity.toCat());
        } else {
            return this.catReactive.findAll()
                    .map(catEntity -> catEntity.toCat());
        }
    }

    private Mono<Void> assertCatNotExist(Integer chip) {
        return this.catReactive.readByChip(chip)
                .flatMap(catEntity -> Mono.error(
                        new ConflictException("Cat with chip " + chip + " already exists ")
                ));
    }

    public Mono<CatEntity> assertCatExist(Integer chip) {
        return this.catReactive.readByChip(chip)
                .switchIfEmpty(Mono.error(new NotFoundException("Cat with chip " + chip + " not found")));
    }
}

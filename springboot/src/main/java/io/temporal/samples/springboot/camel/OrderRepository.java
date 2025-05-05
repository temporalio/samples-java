package io.temporal.samples.springboot.camel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OfficeOrder, Integer> {}

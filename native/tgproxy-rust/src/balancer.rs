use once_cell::sync::Lazy;
use parking_lot::RwLock;
use rand::seq::SliceRandom;
use std::collections::HashMap;

pub struct Balancer {
    domains: Vec<String>,
    dc_to_domain: HashMap<i32, String>,
}

pub static BALANCER: Lazy<RwLock<Balancer>> = Lazy::new(|| RwLock::new(Balancer::new()));

impl Balancer {
    pub fn new() -> Self {
        Self {
            domains: Vec::new(),
            dc_to_domain: HashMap::new(),
        }
    }

    pub fn update_domains_list(&mut self, domains_list: &[String]) {
        let mut current_sorted = self.domains.clone();
        current_sorted.sort();
        let mut new_sorted = domains_list.to_vec();
        new_sorted.sort();

        if current_sorted == new_sorted {
            return;
        }

        self.domains = domains_list.to_vec();
        let mut rng = rand::thread_rng();

        self.dc_to_domain.clear();
        for dc_id in [1, 2, 3, 4, 5, 203] {
            if let Some(domain) = self.domains.choose(&mut rng) {
                self.dc_to_domain.insert(dc_id, domain.clone());
            }
        }
    }

    pub fn update_domain_for_dc(&mut self, dc_id: i32, domain: &str) -> bool {
        if self.dc_to_domain.get(&dc_id).map(|s| s.as_str()) == Some(domain) {
            return false;
        }
        self.dc_to_domain.insert(dc_id, domain.to_string());
        true
    }

    pub fn get_domains_for_dc(&self, dc_id: i32) -> Vec<String> {
        let mut result = Vec::new();
        let current_domain = self.dc_to_domain.get(&dc_id).cloned();
        
        if let Some(ref d) = current_domain {
            result.push(d.clone());
        }

        let mut shuffled = self.domains.clone();
        let mut rng = rand::thread_rng();
        shuffled.shuffle(&mut rng);

        for d in shuffled {
            if Some(&d) != current_domain.as_ref() {
                result.push(d);
            }
        }
        
        result
    }
}

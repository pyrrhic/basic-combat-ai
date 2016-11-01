# basic-combat-ai

Learning Clojure. 

Learned about and implemented behavior tree's, except mine is not visual. So, it loses the real benefit of even having one, but I just wanted to take a stab at the logic part of them. 

Read a ton about entity component systems and made a very basic one. The ecs datastructure just holds a list of entities (maps) and systems (functions). Each time the ecs is updated, the system functions are executed sequentially. Each system takes all the entities, works on the ones it cares about, then returns the updated list of entities. That updated list is then passed on to the next system, and so on.

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

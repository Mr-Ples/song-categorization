## Notes

### Categorization scheme

first attempt at creating a control flow and storage structure for my categorization/recommender system `doc/initial-categorization-structure.json` is a json file (data) that is a specific structure (data type(s)) with embedded control flow (program)

i ended up going with a much simpler data structure `/media/projects/spotify/clj/category-structure.json`, pushing the control flow to the interpreting program (which makes it less “dynamic”)

- data types + operations are programs
- programs can be more or less contained inside the data of the program (the data / interpreter dichotomy isn't real)
- seeing a data structure that defines its own control flow is fascinating
    - on one hand you can imagine the control flow of the data structure only being "understandable" by a specialized interpreter
    - on the other hand you can imagine encoding it in a way that is understood by a class of interpreters
    - at the extreme you can imagine it being understandable by the most general class of interpreters
    - you can also imagine a physical system can act like an interpreter (it can instantiate a class of interpreters)
    - a physical system has a set of instructions it can understand and interpret, e.g. a catalyst can act as an interpreter for a scpecific chemical reaction

### Categorization

the process of categorizing consists of:
1) choose the phenomenon you want to categorize e.g. songs
2) choose N dimensions that are relevant to you (e.g. relistenability, attention demand)
3) place your data points (e.g. songs) in this N dimensional space by iteratively asking where it lands in each dimension seperately

with this data you can:
4) you can use clustering algorithms to project these points in higher dimensional space into clusters in lower dimensional space
    - this is what attention mechanism is doing in transformers
    - saw a paper once where it said it IS kernel PCA for example
5) clusters have meaning because they might be clustering not because of the bias incoded in the decisions of clustering algorithm or data collection but because there is an underlying regularity in nature

if i categorize my thousands of songs and have a dataset of my personal N dimensional model and i want to build a recommender system i would need to interface with other data sets

this is because songs themselves don't have the metadata in the relevant dimensions i want (since no one made that data set) but there are data sets that can be 1) generated automatically (like the technical metadata of songs like tempo etc) and 2) borrowed from existing data sets (like whatever combination of usage patterns spotify's algorithm saves combined with the other meta data of the song like technicals)

so the insight is if i want to move forward i need to find morphisms between my model and the models in other data sets (or categorize every song on earth myself to find the ones i want)

there will be more on this as i research how to extend my model to a recommender system

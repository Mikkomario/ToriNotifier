## Tori.fi Notifier
This a very simple application that enables one to spot new product updates on [Tori.fi](https://www.tori.fi/).  

The current version (v0.2) of this application is command line only, and is only suitable for a 
very specific use-case (firewood purchases at Päijät Häme). 
This is because the current version is merely a prototype.

I might or might not keep expanding this prototype further. So far this has just been a quick side project for me.

### Background - How this app came to be?
Because of the current (2022) energy "crisis", the demand for firewood has increased, making [Tori.fi ](https://www.tori.fi/)
sales (and giveaway) items more sought after. In order to get a good deal, it is necessary to be early on 
the markets, but who wants to keep manually checking the site?

I kind of accidentally challenged myself to create this application, as I was talking with my wife about this 
situation. So, here we are.

In its current form this application merely meets my needs on this area: It looks for new firewood sales and 
opens the browser for me when it has found one. There are no custom parameters, no fancy UI or anything else, just 
the MVP implementation.

### Usage
Build the artifact using [IntelliJ IDEA](https://www.jetbrains.com/idea/). 
Run the Tori-Notifier.jar using command line with: `java -jar Tori-Notifier.jar` 
([JRE 8](https://www.openlogic.com/openjdk-downloads?field_java_parent_version_target_id=416&field_operating_system_target_id=All&field_architecture_target_id=All&field_java_package_target_id=401) 
or later is required)

When you want to close the app, just press enter (i.e. provide any text input).

### Future Improvements
If I end up working on this application, I'll attempt to work on the following features:
1. Customizable search
2. Concurrent searches
3. Graphical user interface
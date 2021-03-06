/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Baritone. If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.command.manager;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.command.ICommand;
import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.exception.CommandUnhandledException;
import baritone.api.command.exception.ICommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.command.manager.ICommandManager;
import baritone.api.command.registry.Registry;
import baritone.command.argument.ArgConsumer;
import baritone.command.argument.CommandArguments;
import baritone.command.defaults.DefaultCommands;
import net.minecraft.util.Pair;

/**
 * The default, internal implementation of {@link ICommandManager}
 *
 * @author Brady
 * @since 9/21/2019
 */
public class CommandManager implements ICommandManager {

	private static final class ExecutionWrapper {

		private ICommand command;
		private String label;
		private ArgConsumer args;

		private ExecutionWrapper(ICommand command, String label, ArgConsumer args) {
			this.command = command;
			this.label = label;
			this.args = args;
		}

		private void execute() {
			try {
				command.execute(label, args);
			} catch (Throwable t) {
				// Create a handleable exception, wrap if needed
				ICommandException exception = t instanceof ICommandException ? (ICommandException) t : new CommandUnhandledException(t);

				exception.handle(command, args.getArgs());
			}
		}

		private Stream<String> tabComplete() {
			try {
				return command.tabComplete(label, args);
			} catch (Throwable t) {
				return Stream.empty();
			}
		}
	}

	public static Pair<String, List<ICommandArgument>> expand(String string) {
		return expand(string, false);
	}

	private static Pair<String, List<ICommandArgument>> expand(String string, boolean preserveEmptyLast) {
		String label = string.split("\\s", 2)[0];
		List<ICommandArgument> args = CommandArguments.from(string.substring(label.length()), preserveEmptyLast);
		return new Pair<>(label, args);
	}

	private final Registry<ICommand> registry = new Registry<>();

	private final Baritone baritone;

	public CommandManager(Baritone baritone) {
		this.baritone = baritone;
		DefaultCommands.createAll(baritone).forEach(registry::register);
	}

	@Override
	public boolean execute(Pair<String, List<ICommandArgument>> expanded) {
		ExecutionWrapper execution = from(expanded);
		if (execution != null) {
			execution.execute();
		}
		return execution != null;
	}

	@Override
	public boolean execute(String string) {
		return this.execute(expand(string));
	}

	private ExecutionWrapper from(Pair<String, List<ICommandArgument>> expanded) {
		String label = expanded.getLeft();
		ArgConsumer args = new ArgConsumer(this, expanded.getRight());

		ICommand command = getCommand(label);
		return command == null ? null : new ExecutionWrapper(command, label, args);
	}

	@Override
	public IBaritone getBaritone() {
		return baritone;
	}

	@Override
	public ICommand getCommand(String name) {
		for (ICommand command : registry.entries) {
			if (command.getNames().contains(name.toLowerCase(Locale.US)))
				return command;
		}
		return null;
	}

	@Override
	public Registry<ICommand> getRegistry() {
		return registry;
	}

	@Override
	public Stream<String> tabComplete(Pair<String, List<ICommandArgument>> expanded) {
		ExecutionWrapper execution = from(expanded);
		return execution == null ? Stream.empty() : execution.tabComplete();
	}

	@Override
	public Stream<String> tabComplete(String prefix) {
		Pair<String, List<ICommandArgument>> pair = expand(prefix, true);
		String label = pair.getLeft();
		List<ICommandArgument> args = pair.getRight();
		if (args.isEmpty())
			return new TabCompleteHelper().addCommands(baritone.getCommandManager()).filterPrefix(label).stream();
		else
			return tabComplete(pair);
	}
}
